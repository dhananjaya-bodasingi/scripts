// Import Azure libraries (make sure to include Azure SDK dependencies)
import com.azure.storage.blob.BlobContainerClientBuilder
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues
import com.azure.storage.blob.sas.BlobSasPermission
import java.time.OffsetDateTime

// Function to generate SAS token
def generateSasToken(storageAccountName, storageAccountKey, containerName) {
    def endpoint = "https://${storageAccountName}.blob.core.windows.net"
    def credential = new com.azure.storage.common.StorageSharedKeyCredential(storageAccountName, storageAccountKey)

    def blobContainerClient = new BlobContainerClientBuilder()
        .endpoint(endpoint)
        .containerName(containerName)
        .credential(credential)
        .buildClient()

    def expiryTime = OffsetDateTime.now().plusDays(7)  // SAS token valid for 7 days

    def sasPermission = new BlobSasPermission()
    sasPermission.read = true
    sasPermission.write = true
    sasPermission.create = true
    sasPermission.add = true

    def sasValues = new BlobServiceSasSignatureValues(expiryTime, sasPermission)
    def sasToken = blobContainerClient.generateSas(sasValues)

    return sasToken
}

// Usage example
def storageAccountName = "your_storage_account_name"
def storageAccountKey = "your_storage_account_key"
def containerName = "your_container_name"

def sasToken = generateSasToken(storageAccountName, storageAccountKey, containerName)
println "Generated SAS Token: ${sasToken}"
