kubectl config get-contexts
kubectl config use-context prod-eastus
kubectl get configmaps -n router-nginx-ns
kubectl edit configmaps -n router-nginx-ns doc-root-config
kubectl get all -n router-nginx-ns
kubectl rollout restart deployment.apps/router-nginx-image -n router-nginx-ns
kubectl get pod -n mama | grep api