#!/bin/bash

# Variables
jenkins_user="YOUR_JENKINS_USERNAME"
jenkins_token="YOUR_JENKINS_API_TOKEN"
slack_webhook_url="YOUR_SLACK_WEBHOOK_URL"

# Jenkins URLs
uat_jenkins_url="https://bitbucket.org/whatfix/whatfix-jenkins/branches/"
ansible_jenkins_url="https://bitbucket.org/whatfix/whatfix-ansible-v2/branches/"
promote_url="https://mamaops.quickolabs.com/jenkins/job/PRODUCTION-all-trigger-job-v2/buildWithParameters"
nginx_promote_url="https://devops.quickolabs.com/view/Build%20Promotions/job/promote-whatfix-nginx-eserver/buildWithParameters"
nginx_deploy_url_us="https://prodops.whatfix.com/view/Production%20Deployments%20WIP/job/PROD-US-East-nginx-Deployment/build"
nginx_deploy_url_eu="https://prodops.whatfix.com/view/Production%20Deployments%20WIP/job/PROD-EU-northeurope-nginx-deployment/build"
cache_purge_url="https://prodops.whatfix.com/job/production-Cloudflare-CachePurge/buildWithParameters"

# Parameters
release_type=$1  # "uat" or "bugfix"
blue_green=$2    # "true" for blue-green, "false" for all-at-once

# Step 1: Sync branches based on release type
if [ "$release_type" == "uat" ]; then
    echo "Syncing development branches to master for UAT release"
    curl -X POST "$uat_jenkins_url"
    curl -X POST "$ansible_jenkins_url"
elif [ "$release_type" == "bugfix" ]; then
    echo "Syncing maintenance branches to master"
    curl -X POST "$uat_jenkins_url"
    curl -X POST "$ansible_jenkins_url"
fi

# Step 2: Promotion Step
promote_parameters="promote_from=$release_type"
if [ "$blue_green" == "true" ]; then
    promote_parameters+="&promote_api=true&blue_green=true&promote_ui=true&promote_editor=true"
else
    promote_parameters+="&promote_api=true&promote_ui=true&promote_editor=true"
fi

curl -X POST "$promote_url" \
    --user $jenkins_user:$jenkins_token \
    --data "$promote_parameters"

# Step 3: Nginx Promotion (E Server)
nginx_parameters="uat"
if [ "$release_type" == "bugfix" ]; then
    nginx_parameters="maintenance"
fi

curl -X POST "$nginx_promote_url" \
    --user $jenkins_user:$jenkins_token \
    --data "promotion_type=$nginx_parameters"

# Step 4: E Server Deployment
curl -X POST "$nginx_deploy_url_us" --user $jenkins_user:$jenkins_token
curl -X POST "$nginx_deploy_url_eu" --user $jenkins_user:$jenkins_token

# Step 5: Cache Purge after deployments
if [ "$release_type" == "uat" ]; then
    curl -X POST "$cache_purge_url" --user $jenkins_user:$jenkins_token --data "prod-us"
else
    curl -X POST "$cache_purge_url" --user $jenkins_user:$jenkins_token --data "prod-eu"
fi

# Step 6: Slack Notification
send_slack_notification() {
    curl -X POST -H 'Content-type: application/json' --data "{\"text\":\"$1\"}" $slack_webhook_url
}

send_slack_notification "Build Promotion completed for $release_type. Promoted API, UI, Editor, and Nginx."
send_slack_notification "E server deployment completed on both US and EU. Waiting for SRE validation."

