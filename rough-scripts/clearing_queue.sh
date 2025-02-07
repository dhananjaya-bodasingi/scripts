redis : for sandbox (10.121.0.12) 
        for UAT.    ( 10.0.0.156 , 10.0.0.155 , 10.0.0.157 )
         for Prod US (10.5.0.9) (use this redis-cli -a whatfix@836)

ssh <ip> 

sudo -i

redis-cli

auth whatfix@836

KEYS DEPLOYMENT_JOB_CLM* or KEYS "DEPLOYMENT_JOB_CLM::< ent_id> "

if you get any output: 

DEL "DEPLOYMENT_JOB_CLM::< ent_id> "