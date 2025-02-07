#!/bin/bash

# Variables - Update these with your details
REGISTRY_API_KEY=".."        # Replace with your Datadog API key
DD_SITE="us3.datadoghq.com"           
TAGS="service:registry,env:produs,datacenter:east" 
DD_ENV="produs"
DD_AGENT_VERSION="7.49.0-1"           # Datadog agent version (adjust as needed)

echo "Starting Datadog Agent installation..."

# Step 1: Download the Datadog Agent installation script
curl -o install_script_agent7.sh https://s3.amazonaws.com/dd-agent/scripts/install_script_agent7.sh
chmod +x install_script_agent7.sh

# Step 2: Run the installation script
bash install_script_agent7.sh

# Step 3: Configure the Datadog agent with the API key, site, and tags
echo "Updating Datadog configuration..."

# Correct the API key configuration in datadog.yaml file
sed -i "s/^api_key:.*/api_key: ${REGISTRY_API_KEY}/" /etc/datadog-agent/datadog.yaml

# Add the site configuration
echo "site: ${DD_SITE}" >> /etc/datadog-agent/datadog.yaml

# Add environment and tags
echo "env: ${DD_ENV}" >> /etc/datadog-agent/datadog.yaml
echo "tags: [${TAGS}]" >> /etc/datadog-agent/datadog.yaml
echo "process_config:" >> /etc/datadog-agent/datadog.yaml
echo "  enabled: true" >> /etc/datadog-agent/datadog.yaml

# Step 4: Create a configuration for disk monitoring (only specific disks)
echo "Setting up disk monitoring for specific disks..."
mkdir -p /etc/datadog-agent/conf.d/disk.d

# Dynamically fetch hostname (assuming it is required for the tag)
HOSTNAME=$(hostname)

# Update the disk configuration with specific disks and dynamic tags
cat <<EOF > /etc/datadog-agent/conf.d/disk.d/conf.yaml
init_config:

instances:
  - use_mount: false  # Disables mount detection
    include:           # Only include these disks
      - '/dev/sdb'
      - '/dev/sdc'
    exclude_re: '/(dev|proc|sys|run|var/lib/docker|tmpfs|overlay|shm|/boot|/root|/mnt/HC_Volume_19824103|/mnt/HC_Volume_16098620)'  # Exclude unnecessary mount points
    tags:
      - service:registry
      - env:${DD_ENV}
      - datacenter:east
      - device:/dev/sdb
      - host:${HOSTNAME}
      - mount_point:/mnt/HC_Volume_19824103

  - use_mount: false  # Second instance for sdc
    include:           
      - '/dev/sdc'
    exclude_re: '/(dev|proc|sys|run|var/lib/docker|tmpfs|overlay|shm|/boot|/root|/mnt/HC_Volume_19824103|/mnt/HC_Volume_16098620)'  # Exclude unnecessary mount points
    tags:
      - service:registry
      - env:${DD_ENV}
      - datacenter:east
      - device:/dev/sdc
      - host:${HOSTNAME}
      - mount_point:/mnt/HC_Volume_16098620
EOF

# Step 5: Create a configuration for the registry service (example: health check for the registry)
# Uncomment and configure this section as needed
## mkdir -p /etc/datadog-agent/conf.d/registry.d

## cat <<EOF > /etc/datadog-agent/conf.d/registry.d/conf.yaml
## init_config:

## instances:
 ## - url: "http://localhost:8080/health"
  ##  timeout: 5
  ##  tags:
   ##   - service:registry
   ##   - env:${DD_ENV}
   ##   - datacenter:east
## EOF

# Step 6: Restart the Datadog agent to apply changes (only first time)
echo "Restarting Datadog Agent for the first time..."
systemctl restart datadog-agent

# Step 7: Enable the Datadog agent to start on boot (in case of VM restart)
echo "Enabling Datadog Agent to start on boot..."
systemctl enable datadog-agent

# Step 8: Confirm that the agent is running
echo "Verifying Datadog Agent status..."
systemctl status datadog-agent

echo "Datadog Agent installation and configuration completed successfully!"


# application : ffc77bb044b53c75f893dbbddcf7decbe2607990

# api_key=d01f0db40b4b984e9195f287aaa5e275
