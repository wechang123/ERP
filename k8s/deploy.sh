#!/bin/bash

# ERP 마이크로서비스 쿠버네티스 배포 스크립트
# Minikube 또는 Kind 사용

echo "=== ERP Kubernetes Deployment ==="

# 1. Docker 이미지 빌드
echo "Building Docker images..."

cd ..

# Employee Service
echo "Building employee-service..."
docker build -t erp/employee-service:latest ./employee-service

# Approval Request Service
echo "Building approval-request-service..."
docker build -t erp/approval-request-service:latest ./approval-request-service

# Approval Processing Service
echo "Building approval-processing-service..."
docker build -t erp/approval-processing-service:latest ./approval-processing-service

# Notification Service
echo "Building notification-service..."
docker build -t erp/notification-service:latest ./notification-service

# Web Service
echo "Building web-service..."
docker build -t erp/web-service:latest ./web-service

cd k8s

# Minikube 사용시 이미지 로드
if command -v minikube &> /dev/null; then
    echo "Loading images to Minikube..."
    minikube image load erp/employee-service:latest
    minikube image load erp/approval-request-service:latest
    minikube image load erp/approval-processing-service:latest
    minikube image load erp/notification-service:latest
    minikube image load erp/web-service:latest
fi

# 2. Kubernetes 리소스 배포
echo "Deploying to Kubernetes..."

# Namespace
kubectl apply -f namespace.yaml

# ConfigMap & Secret
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml

# Infrastructure
echo "Deploying infrastructure..."
kubectl apply -f mysql.yaml
kubectl apply -f mongodb.yaml
kubectl apply -f kafka.yaml

echo "Waiting for infrastructure to be ready..."
sleep 30

# Microservices
echo "Deploying microservices..."
kubectl apply -f employee-service.yaml
kubectl apply -f notification-service.yaml
kubectl apply -f approval-processing-service.yaml
kubectl apply -f approval-request-service.yaml
kubectl apply -f web-service.yaml

echo "=== Deployment Complete ==="
echo "Check status with: kubectl get pods -n erp-system"
echo "Access web UI at: http://localhost:30000 (or minikube service web-service -n erp-system)"
