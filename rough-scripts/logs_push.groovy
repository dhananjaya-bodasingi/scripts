import groovy.json.JsonSlurper
import groovy.transform.Field

@Field Map<String, Map<String, String>> datacenters = [
        'useast_hadoop': [
                'host_ips'     : '10.1.0.78 10.1.0.79',
                'storage_account': 'wfuseastloghadoop',
                'container'     : 'hadoop-logs',
                'sas_token'     : 'sv=2022-11-02&ss=bfqt&srt=sco&sp=rwdlacupiytfx&se=2024-09-26T14:49:23Z&st=2024-09-24T06:49:23Z&spr=https&sig=Yudbo%2F3ype3uzlT7epf48uBtrkWI%2Br24usaVJkXM7No%3D',
                'log_dir'      : '/logsdrive/hadoop/logs',
                'log_prefixes' : 'hadoop-whatfix-datanode hadoop-whatfix-nodemanager'
        ],
        'useast_kafka': [
                'host_ips'     : '10.1.0.80',
                'storage_account': 'wfuseastlogkafka',
                'container'     : 'kafka-logs',
                'sas_token'     : '<your_sas_token>',
                'log_dir'      : '/logsdrive/kafka/logs',
                'log_prefixes' : 'kafka-whatfix'
        ],
        'uswest_cassandra': [
                'host_ips'     : '10.2.0.78',
                'storage_account': 'wfuswestlogcassandra',
                'container'     : 'cassandra-logs',
                'sas_token'     : '<your_sas_token>',
                'log_dir'      : '/logsdrive/cassandra/logs',
                'log_prefixes' : 'cassandra-whatfix'
        ]
]

// Function to install AzCopy if not present
def installAzCopy() {
    println "AzCopy not found. Installing AzCopy..."
    def command = "wget https://aka.ms/downloadazcopy-v10-linux -O downloadazcopy-v10-linux"
    def process = command.execute()
    process.waitFor()

    command = "tar -xvf downloadazcopy-v10-linux"
    process = command.execute()
    process.waitFor()

    command = "sudo cp ./azcopy_linux_amd64_*/azcopy /usr/bin/"
    process = command.execute()
    process.waitFor()

    command = "rm -f downloadazcopy-v10-linux && rm -rf ./azcopy_linux_amd64_*/"
    process = command.execute()
    process.waitFor()

    println "AzCopy installation completed."
}

// Function to check if azcopy is installed, install if not present
def checkAzCopy() {
    def process = "azcopy --version".execute()
    process.waitFor()
    if (process.exitValue() != 0) {
        installAzCopy()
    } else {
        println "AzCopy is already installed."
    }
}

// Function to get the current host's IP address and hostname
def getHostInfo() {
    def hostIP = "hostname -I | awk '{print \$1}'".execute().text.trim()
    def hostname = "hostname".execute().text.trim()
    return [hostIP: hostIP, hostname: hostname]
}

// Function to scan log directory and generate log prefixes dynamically
def getLogPrefixes(String logDir) {
    def logPrefixes = [] as Set
    println "Scanning log directory: $logDir for log prefixes..."
    
    new File(logDir).eachFile { file ->
        if (file.isFile()) {
            def baseName = file.name
            def logPrefix = baseName.split('-')[0..2].join('-')  // Modify this to match your naming convention
            logPrefixes << logPrefix
        }
    }
    
    if (logPrefixes.isEmpty()) {
        println "No log prefixes found in $logDir."
        System.exit(1)
    }

    println "Detected log prefixes: ${logPrefixes.join(', ')}"
    return logPrefixes
}

// Function to sync logs to Azure Blob storage using azcopy
def syncLogsToAzure(String logPrefix, String storageAccount, String container, String sasToken, String logDir, String hostname) {
    def currentDate = new Date().format('yyyy-MM-dd')

    println "Processing log files for prefix: $logPrefix"

    def logFiles = new File(logDir).listFiles().findAll { it.name.startsWith("$logPrefix-$hostname.log.") }
    logFiles.each { file ->
        def logType = logPrefix.split('-')[2]
        def azureLogPath = "${storageAccount}/${container}/${currentDate}/${hostname}/${logType}/"

        println "Syncing $file to Azure under ${azureLogPath}"

        def command = "azcopy copy \"$file\" \"https://${storageAccount}.blob.core.windows.net/${container}/${currentDate}/${hostname}/${logType}/?${sasToken}\" --recursive=true"
        def process = command.execute()
        process.waitFor()

        if (process.exitValue() == 0) {
            println "Successfully synced $file to ${azureLogPath}"
        } else {
            println "Error: Failed to sync $file"
        }
    }
}

// Function to check if the current host's IP matches any of the configured datacenter's IPs
def checkIpMatch(String hostIP, List<String> hostIPs) {
    hostIPs.each { componentHostIP ->
        println "Checking if host IP matches component IP: $componentHostIP"
        if (hostIP == componentHostIP) {
            return true // Match found
        }
    }
    return false // No match
}

// Main execution
checkAzCopy()
def hostInfo = getHostInfo()
def hostIP = hostInfo.hostIP
def hostname = hostInfo.hostname

def matchFound = false

datacenters.each { dcComponent, config ->
    def dc = dcComponent.split('_')[0]
    def component = dcComponent.split('_')[1]

    println "Processing datacenter: $dc, component: $component"

    def hostIPsArray = config.host_ips.split(' ')

    if (checkIpMatch(hostIP, hostIPsArray)) {
        matchFound = true
        println "Matched Data Center: $dc, Component: $component"

        def logDir = config.log_dir
        if (!new File(logDir).exists()) {
            println "Error: Log directory $logDir not found."
            System.exit(1)
        }

        def logPrefixes = getLogPrefixes(logDir) // Scan the log directory and generate log prefixes

        logPrefixes.each { logPrefix ->
            syncLogsToAzure(logPrefix, config.storage_account, config.container, config.sas_token, logDir, hostname)
        }

        System.exit(0)
    }
}

if (!matchFound) {
    println "Error: No matching component found for host IP: $hostIP"
    System.exit(1)
}
