#!/bin/bash

# Set namespace variable
NAMESPACE=keycloak

echo " 1) Applying Namespace..."
kubectl apply -f namespace.yaml

echo " 2) Applying Secrets..."
kubectl apply -f secrets.yaml

echo " 3) Applying Persistent Volume Claims..."
kubectl apply -f postgres-pvc.yaml
# kubectl apply -f keycloak-pvc.yaml   # Uncomment if you want to use persistent storage for Keycloak

echo " 4) Deploying Postgres..."
kubectl apply -f postgres-deployment.yaml
kubectl apply -f postgres-service.yaml

# Wait until Postgres pod is ready
echo "⏳ Waiting for Postgres pod to be ready..."
kubectl wait --namespace $NAMESPACE \
  --for=condition=ready pod -l app=keycloak-postgres --timeout=120s

echo " 5) Creating Keycloak Realm ConfigMap..."
kubectl create configmap keycloak-realm \
  --from-file=entlrealm-export.json=./keycloak/entlrealm-export.json \
  -n $NAMESPACE \
  --dry-run=client -o yaml | kubectl apply -f -

echo " 6) Deploying Keycloak..."
kubectl apply -f keycloak-deployment.yaml
kubectl apply -f keycloak-service.yaml

# Wait until Keycloak pod is ready
echo "Waiting for Keycloak pod to be ready..."
kubectl wait --namespace $NAMESPACE \
  --for=condition=ready pod -l app=keycloak --timeout=180s

# echo " 7) Deploying Ingress..."
# kubectl apply -f keycloak-ingress.yaml -n $NAMESPACE

echo "Deployment completed!"
kubectl get pods -n $NAMESPACE
kubectl get svc -n $NAMESPACE
kubectl get ingress -n $NAMESPACE
