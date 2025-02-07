#!/bin/bash

# Declare datacenters and components configurations
declare -A datacenters

# East US :
datacenters["useast_hadoop"]="host_ips='10.1.0.78 10.1.0.79 10.1.0.80' storage_account='wfuseastloghadoop' container='hadoop-logs' sas_token='sv=2022-11-02&ss=bfqt&srt=sco&sp=rwdlacupiytfx&se=2024-09-26T14:49:23Z&st=2024-09-24T06:49:23Z&spr=https&sig=Yudbo%2F3ype3uzlT7epf48uBtrkWI%2Br24usaVJkXM7No%3D' log_dir='/logsdrive/hadoop/logs' "
datacenters["useast_kafka"]="host_ips='10.1.0.72 10.1.0.73 10.1.0.71' storage_account='wfuseastlogkafka' container='kafka-service' sas_token='sv=2022-11-02&ss=bfqt&srt=sco&sp=rwdlacupiytfx&se=2024-09-28T15:15:47Z&st=2024-09-24T07:15:47Z&spr=https&sig=DY%2FV4viebFcz%2Bah%2FYn0pv%2BaynS%2FNrq5zLYvyV2SXC3M%3D' log_dir='/logsdrive/kafka/logs'"

# West US :
datacenters["uswest_cassandra"]="host_ips='10.2.0.78' storage_account='wfuswestlogcassandra' container='cassandra-logs' sas_token='<your_sas_token>' log_dir='/logsdrive/cassandra/logs' "

# Function to install AzCopy if not present
install_azcopy() {
    echo "AzCopy not found. Installing AzCopy..."
    
    # Download AzCopy v10 for Linux
    wget https://aka.ms/downloadazcopy-v10-linux -O downloadazcopy-v10-linux
    
    # Extract the downloaded archive
    tar -xvf downloadazcopy-v10-linux
    
    # Make AzCopy executable and move it to /usr/bin
    sudo chmod 755 ./azcopy_linux_amd64_*/azcopy
    sudo cp ./azcopy_linux_amd64_*/azcopy /usr/bin/
    
    # Clean up downloaded files
    rm -f downloadazcopy-v10-linux
    rm -rf ./azcopy_linux_amd64_*/
    
    echo "AzCopy installation completed."
}

# Function to check if azcopy is installed, install if not present
check_azcopy() {
    if ! command -v azcopy &> /dev/null; then
        install_azcopy
    else
        echo "AzCopy is already installed."
    fi
}

# Function to get the current host's IP address and hostname
get_host_info() {
    host_ip=$(hostname -I | awk '{print $1}')
    hostname=$(hostname)
    echo "Host IP: $host_ip"
    echo "Hostname: $hostname"
}

# Function to scan log directory and generate log prefixes dynamically
get_log_prefixes() {
    local log_dir="$1"
    log_prefixes=()
    
    echo "Scanning log directory: $log_dir for log prefixes..."
    for file in "$log_dir"/*; do
        if [[ -f "$file" ]]; then
            base_name=$(basename "$file")
            log_prefix=$(echo "$base_name" | cut -d'-' -f1-3)  # Modify this to match your naming convention
            log_prefixes+=("$log_prefix")
        fi
    done

    # Remove duplicates and sort prefixes
    log_prefixes=($(echo "${log_prefixes[@]}" | tr ' ' '\n' | sort -u))
    
    if [[ ${#log_prefixes[@]} -eq 0 ]]; then
        echo "No log prefixes found in $log_dir."
        exit 1
    fi

    echo "Detected log prefixes: ${log_prefixes[@]}"
}

# Function to sync logs to Azure Blob storage using azcopy
sync_logs_to_azure() {
    local log_prefix="$1"
    local storage_account="$2"
    local container="$3"
    local sas_token="$4"
    local log_dir="$5"
    local hostname="$6"

    echo "Processing log files for prefix: $log_prefix"

    for filename in ${log_dir}/${log_prefix}-${hostname}.log.*; do
        if [[ -f "$filename" ]]; then
            log_type=$(echo "$log_prefix" | cut -d'-' -f3)

            # Extract date from the filename
            if [[ "$filename" =~ ([0-9]{4}-[0-9]{2}-[0-9]{2}) ]]; then
                file_date="${BASH_REMATCH[1]}"  # Extract YYYY-MM-DD format
            elif [[ "$filename" =~ ([0-9]{2}-[0-9]{2}-[0-9]{4}) ]]; then
                file_date="${BASH_REMATCH[1]}"   # Handle DD-MM-YYYY format
                file_date=$(echo "$file_date" | awk -F '-' '{print $3 "-" $2 "-" $1}')  # Convert to YYYY-MM-DD
            elif [[ "$filename" =~ ([0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{2}) ]]; then
                file_date="${BASH_REMATCH[1]:0:10}"  # Extract only the date part (YYYY-MM-DD)
            else
                echo "Warning: No valid date found in filename $filename"
                continue
            fi

            azure_log_path="${storage_account}/${container}/${file_date}/${hostname}/${log_type}/"

            echo "Syncing $filename to Azure under ${azure_log_path}"

            azcopy copy "$filename" "https://${storage_account}.blob.core.windows.net/${container}/${file_date}/${hostname}/${log_type}/?${sas_token}" --recursive=true

            if [[ $? -eq 0 ]]; then
                echo "Successfully synced $filename to ${azure_log_path}"
            else
                echo "Error: Failed to sync $filename"
            fi
        else
            echo "No files found matching: ${filename}"
        fi
    done
}

# Function to check if the current host's IP matches any of the configured datacenter's IPs
check_ip_match() {
    local host_ips_array=("$@")
    for component_host_ip in "${host_ips_array[@]}"; do
        echo "Checking if host IP matches component IP: $component_host_ip"
        if [[ "$host_ip" == "$component_host_ip" ]]; then
            return 0 # Match found
        fi
    done
    return 1 # No match
}

# Main execution
check_azcopy
get_host_info

match_found=false

for dc_component in "${!datacenters[@]}"; do
    dc="${dc_component%_*}"
    component="${dc_component#*_}"

    echo "Processing datacenter: $dc, component: $component"

    eval "${datacenters[$dc_component]}" # Populate variables like host_ips, storage_account, etc.

    IFS=' ' read -r -a host_ips_array <<< "$host_ips"

    if check_ip_match "${host_ips_array[@]}"; then
        match_found=true
        echo "Matched Data Center: $dc, Component: $component"

        if [[ ! -d "$log_dir" ]]; then
            echo "Error: Log directory $log_dir not found."
            exit 1
        fi

        get_log_prefixes "$log_dir"  # Scan the log directory and generate log prefixes

        for log_prefix in "${log_prefixes[@]}"; do
            sync_logs_to_azure "$log_prefix" "$storage_account" "$container" "$sas_token" "$log_dir" "$hostname"
        done

        exit 0
    fi
done

if [[ "$match_found" = false ]]; then
    echo "Error: No matching component found for host IP: $host_ip"
    exit 1
fi
