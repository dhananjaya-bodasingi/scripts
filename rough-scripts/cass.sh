#!/bin/bash


# Script to take snapshots/backups and ship them to AZURE BLOB


export PATH=/sbin:/bin:/usr/sbin:/usr/bin
LOG_FILE="/tmp/cassandra_backup.log"

# create a log file
touch $LOG_FILE
echo "" > $LOG_FILE


# Cassandra IPs
cass_sandbox_ips=("10.121.0.4" "10.121.0.5" "10.121.0.6", "127.0.0.1")
cass_uat_ips=(
	"10.0.0.45"   "10.0.0.47"   "10.0.0.48" 								# authoring UAT US EAST
	"10.141.0.13" "10.141.0.12" "10.141.0.10" 								# authoring UAT US WEST
	"10.0.0.60"   "10.0.0.61"   "10.0.0.62" 								# enduser UAT US EAST
	"10.141.0.18" "10.141.0.16" "10.141.0.17"								# enduser UAT US WEST
	"10.0.0.92"   "10.0.0.93"   "10.0.0.94" 								# object UAT US EAST
	"10.191.0.35" "10.191.0.36" "10.191.0.37"								# object UAT US WEST
	)
cass_prod_us_ips=(
	"10.1.0.4" 	  "10.1.0.6" 	"10.1.0.7" 	  "10.1.0.8" 	"10.1.0.9"		# authoring PROD US EAST
	"10.181.0.7"  "10.181.0.5"  "10.181.0.10" "10.181.0.9"  "10.181.0.12"   # authoring PROD US WEST
	"10.1.0.31"   "10.1.0.32"   "10.1.0.33"   "10.1.0.34"   "10.1.0.35"		# enduser PROD US EAST
	"10.181.0.13" "10.181.0.4"  "10.181.0.11" "10.181.0.8"  "10.181.0.6"    # enduser PROD US WEST
	"10.1.0.88"   "10.1.0.87"   "10.1.0.89"									# object PROD US EAST
	"10.181.0.44" "10.181.0.49" "10.181.0.59"								# object PROD US WEST
	"10.61.0.4"   "10.61.0.5"   "10.61.0.6"									# doubleslash PROD US EAST
	)
cass_prod_eu_ips=(
	"10.28.0.4"   "10.28.0.5"   "10.28.0.6"									# PROD EU SWITZERLAND NORTH
	"10.171.0.6"  "10.171.0.10" "10.171.0.11"								# PROD EU NORTH EUROPE
	"10.22.0.189" "10.22.0.19"  "10.22.0.190" 								# object PROD EU SIWTZERLAND NORTH
)

# Azure Blob Details
sas_token_sandbox="?sv=2022-11-02&ss=bfqt&srt=sco&sp=rwdlacupiytfx&se=2024-04-22T15:38:40Z&st=2024-04-22T07:38:40Z&spr=https&sig=oDH7YYUgJ7tKK%2FERJSnKlAti1f8PlvzU%2FZMZ2OaXR5Y%3D"
sas_token_uat="?sv=2022-11-02&ss=bfqt&srt=sco&sp=rwdlacupiytfx&se=2030-12-30T20:21:39Z&st=2024-04-22T12:21:39Z&spr=https&sig=0eUbGszFJJ3l2T06wv91aVWtrmXV%2FRO0pAJ4vr4T6CU%3D"
sas_token_prod_us="?sv=2022-11-02&ss=bfqt&srt=sco&sp=rwlacupitfx&se=2027-03-11T15:16:25Z&st=2024-05-02T07:16:25Z&spr=https&sig=ECYcB%2BLX%2BdGqPNiVxksLJzc9DJ41aezxlabTaTiU7ag%3D"
sas_token_prod_eu="TBG"

storage_account_sandbox="sandboxcassandrabackup"
storage_account_uat="stcassandrabackupuat"
storage_account_prod_us="wfuseastbkpcass"
storage_account_prod_eu="TBG"

BACKUP_BLOB_DIR="backups"
SNAPSHOT_BLOB_DIR="snapshots"
SCHEMA_BLOB_DIR="schema"

#Helper function to set blob details
function set_env_blob_details() {
	echo "IP belongs to $1.. Configuring $1 AZ storage settings" >> $LOG_FILE
	sas_token_var="sas_token_${1}"
	storage_account_var="storage_account_${1}"
	SAS_TOKEN=${!sas_token_var}
	STORAGE_ACCOUNT=${!storage_account_var}
}


#Function to set the sas tokens and bucket URL
function az_storage_details() {
	if [[ " ${cass_sandbox_ips[@]} " =~ " ${HOSTNAME} " ]]; then
		set_env_blob_details "sandbox"
	elif [[ " ${cass_uat_ips[@]} " =~ " ${HOSTNAME} " ]]; then
		set_env_blob_details "uat"
	elif [[ " ${cass_prod_us_ips[@]} " =~ " ${HOSTNAME} " ]]; then
		set_env_blob_details "prod_us"
	elif [[ " ${cass_prod_eu_ips[@]} " =~ " ${HOSTNAME} " ]]; then
		set_env_blob_details "prod_eu"
	else
		echo "IP does not belong to any group.. Check the IP again!" >> $LOG_FILE
		exit 1
	fi
}


#Function to take a snapshot
function take_snapshot() {
	echo "Clearing previous snapshots.." >> $LOG_FILE

	#Clear previous snapshots before taking a new one
	$NODETOOL -h 127.0.0.1 clearsnapshot --all

	if [[ $? != 0 ]]; then
		echo "Can't clear snapshots" >> $LOG_FILE
		exit
	fi
	echo "Clearing previous snapshots.. DONE" >> $LOG_FILE

	echo "Taking snapshot.." >> $LOG_FILE
	# Take a snapshot and tag it with $SNAME
	$NODETOOL -h 127.0.0.1 snapshot -t $SNAME

	if [[ $? != 0 ]]; then
		echo "Can't take snapshots" >> $LOG_FILE
		exit
	fi
	echo "Taking snapshot.. DONE" >> $LOG_FILE

	cd $DATADIR

	SFILES=$(ls -1 -d */*/snapshots/$SNAME)

	# upload the data
	for f in $SFILES; do
	echo "uploading snapshot file: ${f}" >> $LOG_FILE
		$AZCOPY copy "$f/*" "https://${STORAGE_ACCOUNT}.blob.core.windows.net/${SNAPSHOT_BLOB_DIR}/${HOSTNAME}/${DATE}/${f}/${SAS_TOKEN}" --recursive=true
	done

	# clear incremental backup files; These are not important after snapshot
	BFILES=$(ls -1 -d $DATADIR/*/*/backups/)
	for f in $BFILES; do
		echo "Removing: $f" >> $LOG_FILE
		rm -f $f*
	done


	# Backup Schema
	echo "Saving keyspaces in /tmp/schemas.txt.." >> $LOG_FILE
	CASIP=$(hostname -I)
	$CQLSH $CASIP --connect-timeout 45 -u cassandra -p cassandra -e "DESC KEYSPACES" >/tmp/schemas.txt
	echo "Saving keyspaces in /tmp/schemas.txt.. DONE" >> $LOG_FILE

	echo "Pusing Keyspace schema to Blob.." >> $LOG_FILE
	for keyspace in $(cat /tmp/schemas.txt); do
		$CQLSH $CASIP --connect-timeout 45 -u cassandra -p cassandra -e "DESC KEYSPACE  $keyspace" >"/tmp/$keyspace.cql"
		$AZCOPY copy "/tmp/$keyspace.cql" "https://${STORAGE_ACCOUNT}.blob.core.windows.net/${SCHEMA_BLOB_DIR}/${HOSTNAME}/${DATE}/${keyspace}.cql/${SAS_TOKEN}"
	done
	echo "Pusing Keyspace schema to Blob.. DONE" >> $LOG_FILE

	#Clear the current snapshot after uploading to blob
	echo "Clearing snapshot after Azure upload.." >> $LOG_FILE
	$NODETOOL -h 127.0.0.1 clearsnapshot --all

	if [[ $? != 0 ]]; then
		echo "Can't clear snapshots after upload to Azure" >> $LOG_FILE
		exit
	fi
	echo "Clearing snapshot after Azure upload.. DONE" >> $LOG_FILE

	#Backup tokens
	# $NODETOOL ring | grep $CASIP | awk '{print $NF ", "}' | xargs >/tmp/tokens.txt
	# $AZCOPY copy "/tmp/tokens.txt" "https://${STORAGE_ACCOUNT}.blob.core.windows.net/${SCHEMA_BLOB_DIR}/${HOSTNAME}/${DATE}/${keyspace}.cql/${SAS_TOKEN}"
}

function copy_backups() {

	echo "Nodetool flush.." >> $LOG_FILE
	#Run nodetool flush to flush memtables to disk before taking backup
	$NODETOOL flush

	echo "Nodetool flush.. DONE" >> $LOG_FILE
	cd $DATADIR

	BFILES=$(ls -1 -d */*/backups)
	echo "Found incremental backups:" >> $LOG_FILE
	echo $BFILES >> $LOG_FILE

	echo "Uploading incremental backups to Blob.." >> $LOG_FILE
	# upload the data
	for f in $BFILES; do
		echo "uploading increment backup file: ${f}" >> $LOG_FILE
		$AZCOPY copy "$f/*" "https://${STORAGE_ACCOUNT}.blob.core.windows.net/${BACKUP_BLOB_DIR}/${HOSTNAME}/${DATE}/${f}/${SAS_TOKEN}" --recursive=true
	done
	echo "Uploading incremental backups to Blob.. DONE" >> $LOG_FILE

}

# Entry point ---------------------------------------- //

# Variables
DATE=$(date +%Y%m%d)
DAY=$(date ++%Y%m%d)
DAYOFWEEK=$(date +%u)
WEEKOFYEAR=$(date +%V)
HOSTNAME=$(hostname -I | cut -d' ' -f1)  # Takes the first IP address

# Azure Blob Variables
AZCOPY="/usr/bin/azcopy"


# Cassandra Variables
DATADIR="/mnt/cassandra/data" #datadrives of nodes (TBG)
NODETOOL=$(which nodetool)
CQLSH=$(which cqlsh)
SNAME="snapshot-$DATE"

az_storage_details

if [ "${DAYOFWEEK}" -eq 7 ]; then
	echo "Taking Snapshot.." >> $LOG_FILE
	take_snapshot
	echo "Taking Snapshot.. DONE" >> $LOG_FILE
else
	echo "Taking incremental backup.." >> $LOG_FILE
	copy_backups
	echo "Taking incremental backup.. DONE" >> $LOG_FILE
fi