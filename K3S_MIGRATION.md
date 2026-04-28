# K3s 마이그레이션 실행계획

> 작성일: 2026-04-28  
> 현재 스택: Spring Boot 2.7.9 + PostgreSQL + Redis + 로컬 파일 스토리지 (단일 서버)  
> 목표: 베어메탈 멀티노드 K3s 클러스터로 전환

---

## 전제 조건 및 환경

```
PC 구성 (예시 IP, 실제 LAN IP로 교체):
├── PC1 (마스터) - 192.168.0.10  : K3s 컨트롤 플레인 + Nginx Ingress + Cloudflare Tunnel
├── PC2 (워커1) - 192.168.0.11  : 애플리케이션 서버 (Spring Boot, Frontend)
└── PC3 (워커2) - 192.168.0.12  : DB (PostgreSQL) + 모니터링 (Prometheus, Grafana)

네트워크:
├── 같은 LAN (유선 권장)
├── 공인 IP 1개
└── Cloudflare Tunnel로 외부 접근 (포트포워딩 불필요)

OS: Ubuntu 22.04 LTS (모든 노드 동일)
도메인: se-board.kumoh.ac.kr (또는 보유 도메인, Cloudflare DNS 관리)
```

---

## Phase 0. 사전 준비 (K3s 설치 전)

> 현재 서버를 유지하면서 진행. 기존 서비스 중단 없음.

### 0-1. 환경변수 분리 (하드코딩 제거)

현재 `application-local.yml`에 하드코딩된 값들을 환경변수로 분리한다.

**application-prod.yml 작성 (git에 커밋 가능, secrets 미포함):**

```yaml
server:
  port: 8080

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: https://kauth.kakao.com/.well-known/jwks.json
      client:
        registration:
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            client-authentication-method: POST
            authorization-grant-type: authorization_code
            client-name: 카카오
            redirect-uri: ${KAKAO_REDIRECT_URI}
            scope: openid,profile_nickname,account_email
        provider:
          kakao:
            issuer-uri: https://kauth.kakao.com
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            jwk-set-uri: https://kauth.kakao.com/.well-known/jwks.json
            user-name-attribute: id

  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB
      max-request-size: 30MB

  datasource:
    url: ${DB_URL}                       # jdbc:postgresql://postgres:5432/se_new
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  redis:
    host: ${REDIS_HOST}                  # redis.se-board.svc.cluster.local
    port: ${REDIS_PORT:6379}

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    database-platform: org.hibernate.dialect.PostgreSQLDialect

jwt:
  secret: ${JWT_SECRET}
  header: "Authorization"
  expiration_time: 10
  token_prefix: "Bearer"

mail:
  host: "smtp.naver.com"
  addr: "naver.com"
  port: 587
  username: ${MAIL_USERNAME}
  password: ${MAIL_PASSWORD}

frontend:
  url: ${FRONTEND_URL}                   # https://se-board.kumoh.ac.kr

system_account:
  password: ${SYSTEM_ACCOUNT_PASSWORD}

logging.level:
  root: INFO
  com.seproject: INFO

storage:
  rootPath: ${STORAGE_ROOT_PATH:/app/files}
  urlRootPath: /files

management:
  endpoints:
    web:
      exposure:
        include: "health,info,metrics,prometheus"
  endpoint:
    metrics:
      enabled: true
    prometheus:
      enabled: true
```

**.gitignore 수정:** `*.yml` 규칙에 예외 추가

```gitignore
*.yml
!src/main/resources/application-prod.yml
!.github/**/*.yml
```

### 0-2. 백엔드 Dockerfile 작성 (멀티스테이지 빌드)

```dockerfile
# SEBoard_BE/Dockerfile

# ---- Build Stage ----
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 캐싱 레이어
RUN ./gradlew dependencies --no-daemon 2>/dev/null || true

COPY src src
RUN ./gradlew bootJar -x test -x asciidoctor --no-daemon

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN mkdir -p /app/files

COPY --from=builder /app/build/libs/seboard-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
```

### 0-3. 프론트엔드 Dockerfile 작성

SE-FE는 pnpm 모노레포 구조이므로 주의.

```dockerfile
# SE-FE/Dockerfile

# ---- Build Stage ----
FROM node:18-alpine AS builder
WORKDIR /app

# pnpm 설치
RUN npm install -g pnpm

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY apps/se-board/package.json apps/se-board/
COPY packages/ packages/

RUN pnpm install --frozen-lockfile

COPY apps/se-board apps/se-board

# 빌드 시 API 서버 URL 환경변수 주입
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

RUN pnpm --filter se-board build

# ---- Runtime Stage ----
FROM nginx:alpine
COPY --from=builder /app/apps/se-board/dist /usr/share/nginx/html
COPY apps/se-board/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

**SE-FE/apps/se-board/nginx.conf 작성:**

```nginx
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    # React Router를 위한 SPA fallback
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 정적 파일 캐싱
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    gzip on;
    gzip_types text/plain text/css application/json application/javascript;
}
```

### 0-4. 로컬 Docker 동작 확인

K3s에 올리기 전에 로컬에서 컨테이너로 먼저 검증한다.

```bash
# 백엔드 이미지 빌드 & 실행
cd SEBoard_BE
docker build -t se-board-be:test .

docker run --rm -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/se_new \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=1234 \
  -e REDIS_HOST=host.docker.internal \
  -e REDIS_PORT=6379 \
  -e JWT_SECRET=0077d9df52... \
  -e MAIL_USERNAME=laon198@naver.com \
  -e MAIL_PASSWORD=... \
  -e FRONTEND_URL=http://localhost:3000 \
  -e SYSTEM_ACCOUNT_PASSWORD=1234 \
  -e KAKAO_CLIENT_ID=... \
  -e KAKAO_CLIENT_SECRET=... \
  -e KAKAO_REDIRECT_URI=http://localhost:8080/login/oauth2/code/kakao \
  se-board-be:test

# 기본 헬스체크
curl http://localhost:8080/actuator/health
# {"status":"UP"} 확인
```

### 0-5. GitHub Actions 워크플로우 작성

**CI (테스트, PR + push 시 실행):**

```yaml
# .github/workflows/ci.yml
name: CI

on:
  pull_request:
    branches: [main, dev]
  push:
    branches: [main, dev]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: gradle-${{ runner.os }}-${{ hashFiles('**/*.gradle*') }}

      - run: chmod +x gradlew

      # application-test.yml이 H2 사용하므로 별도 DB 불필요
      - run: ./gradlew test --no-daemon
        env:
          SPRING_PROFILES_ACTIVE: test
```

**CD (main push 시 이미지 빌드·푸시·배포):**

```yaml
# .github/workflows/cd.yml
name: CD

on:
  push:
    branches: [main]
    paths-ignore:
      - '*.md'

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    outputs:
      image-tag: ${{ steps.vars.outputs.sha }}

    steps:
      - uses: actions/checkout@v4

      - name: Get short SHA
        id: vars
        run: echo "sha=$(git rev-parse --short HEAD)" >> $GITHUB_OUTPUT

      - uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - uses: docker/setup-buildx-action@v3

      - uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ steps.vars.outputs.sha }}
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy:
    runs-on: ubuntu-latest
    needs: build-and-push
    environment: production       # GitHub Environments 보호 규칙 적용

    steps:
      - uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.K3S_HOST }}       # PC1 공인IP 또는 Cloudflare Tunnel 주소
          username: ${{ secrets.K3S_USER }}
          key: ${{ secrets.K3S_SSH_KEY }}
          script: |
            kubectl set image deployment/seboard-backend \
              seboard-backend=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ needs.build-and-push.outputs.image-tag }} \
              -n seboard
            kubectl rollout status deployment/seboard-backend -n seboard --timeout=180s
```

**필요한 GitHub Secrets:**

| Secret 이름 | 내용 |
|------------|------|
| `K3S_HOST` | PC1 공인IP 또는 SSH 접근 주소 |
| `K3S_USER` | SSH 사용자명 (예: `ubuntu`) |
| `K3S_SSH_KEY` | SSH 개인키 (`~/.ssh/id_rsa` 내용) |

### Phase 0 완료 기준

```
✅ docker run으로 Spring Boot가 정상 기동
✅ /actuator/health → {"status":"UP"}
✅ main push 시 GHCR에 이미지 자동 업로드 확인
   ghcr.io/laon198/seboard-be:latest 존재
```

---

## Phase 1. K3s 클러스터 구성

### 1-1. 모든 노드 공통 설정

각 PC에 Ubuntu 22.04 LTS를 새로 설치한 뒤 아래를 실행한다.

```bash
# 패키지 업데이트
sudo apt update && sudo apt upgrade -y

# swap 비활성화 (K8s 요구사항)
sudo swapoff -a
sudo sed -i '/ swap / s/^\(.*\)$/#\1/g' /etc/fstab

# 방화벽 설정 (K3s 필요 포트)
sudo ufw allow ssh
sudo ufw allow 6443/tcp   # K3s API Server
sudo ufw allow 10250/tcp  # Kubelet
sudo ufw allow 8472/udp   # Flannel VXLAN (노드간 통신)
sudo ufw allow 51820/udp  # WireGuard (암호화 통신, 옵션)
sudo ufw enable

# 고정 IP 설정 (/etc/netplan/00-installer-config.yaml)
# 각 노드별로 IP 다르게 설정
sudo tee /etc/netplan/00-installer-config.yaml <<EOF
network:
  version: 2
  ethernets:
    ens33:                  # 실제 NIC 이름으로 교체 (ip addr 명령으로 확인)
      dhcp4: false
      addresses: [192.168.0.10/24]   # 각 노드 IP
      gateway4: 192.168.0.1
      nameservers:
        addresses: [8.8.8.8, 1.1.1.1]
EOF
sudo netplan apply
```

### 1-2. 마스터 노드 설치 (PC1)

```bash
# K3s 설치 (Traefik 비활성화 → Nginx Ingress 별도 설치)
curl -sfL https://get.k3s.io | sh -s - \
  --write-kubeconfig-mode 644 \
  --disable traefik \
  --node-ip 192.168.0.10

# 설치 확인
sudo kubectl get nodes
# NAME   STATUS   ROLES                  AGE
# pc1    Ready    control-plane,master   1m

# 워커 조인용 토큰 메모
sudo cat /var/lib/rancher/k3s/server/node-token
# K10xxx...

# kubeconfig 로컬 복사 (개발 PC에서 kubectl 사용 시)
# scp ubuntu@192.168.0.10:/etc/rancher/k3s/k3s.yaml ~/.kube/config
# 복사 후 server: https://127.0.0.1:6443 → https://192.168.0.10:6443 로 변경
```

### 1-3. 워커 노드 조인 (PC2, PC3)

```bash
# PC2에서 실행
curl -sfL https://get.k3s.io | \
  K3S_URL=https://192.168.0.10:6443 \
  K3S_TOKEN=<마스터토큰> \
  sh -s - --node-ip 192.168.0.11

# PC3에서 실행
curl -sfL https://get.k3s.io | \
  K3S_URL=https://192.168.0.10:6443 \
  K3S_TOKEN=<마스터토큰> \
  sh -s - --node-ip 192.168.0.12

# 마스터에서 전체 노드 확인
kubectl get nodes -o wide
# NAME   STATUS   ROLES                  AGE   INTERNAL-IP
# pc1    Ready    control-plane,master   5m    192.168.0.10
# pc2    Ready    <none>                 2m    192.168.0.11
# pc3    Ready    <none>                 1m    192.168.0.12
```

### 1-4. 노드 라벨링

```bash
# 어느 노드에 어떤 서비스를 배치할지 라벨 지정
kubectl label node pc2 role=app
kubectl label node pc3 role=database
kubectl label node pc3 role=monitoring

# 확인
kubectl get nodes --show-labels
```

### 1-5. Nginx Ingress Controller 설치

```bash
# 베어메탈용 Nginx Ingress 설치
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/baremetal/deploy.yaml

# 설치 확인 (Running 될 때까지 대기)
kubectl rollout status deployment/ingress-nginx-controller -n ingress-nginx

# NodePort 확인 (MetalLB 없을 경우 이 포트로 접근)
kubectl get svc -n ingress-nginx
```

### 1-6. MetalLB 설치 (베어메탈 LoadBalancer)

```bash
# MetalLB 설치
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/v0.14.5/config/manifests/metallb-native.yaml

# 설치 완료 대기
kubectl rollout status deployment/controller -n metallb-system

# LAN에서 사용할 IP 대역 할당 (라우터 DHCP 범위 밖으로 설정)
cat <<EOF | kubectl apply -f -
apiVersion: metallb.io/v1beta1
kind: IPAddressPool
metadata:
  name: default-pool
  namespace: metallb-system
spec:
  addresses:
    - 192.168.0.200-192.168.0.210   # LAN 대역에 맞게 수정
---
apiVersion: metallb.io/v1beta1
kind: L2Advertisement
metadata:
  name: default
  namespace: metallb-system
EOF
```

### Phase 1 완료 기준

```
✅ kubectl get nodes → 3개 모두 Ready
✅ ingress-nginx pod → Running
✅ metallb pod → Running
✅ kubectl get svc -n ingress-nginx → EXTERNAL-IP 할당됨 (192.168.0.200 등)
```

---

## Phase 2. 스토리지 구성 (Longhorn)

Longhorn은 워커 노드들의 디스크를 묶어 분산 스토리지를 제공한다.  
PostgreSQL 데이터, 파일 업로드, 백업이 여기에 저장된다.

### 2-1. 사전 요구사항 (모든 노드)

```bash
# 모든 노드에서 실행
sudo apt install -y open-iscsi nfs-common
sudo systemctl enable --now iscsid

# 확인
sudo systemctl status iscsid   # active (running) 이어야 함
```

### 2-2. Helm으로 Longhorn 설치

```bash
# Helm 설치 (없으면)
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

helm repo add longhorn https://charts.longhorn.io
helm repo update

helm install longhorn longhorn/longhorn \
  --namespace longhorn-system \
  --create-namespace \
  --set defaultSettings.defaultReplicaCount=2   # 데이터를 2개 노드에 복제

# 설치 확인 (모든 pod Running 될 때까지 수 분 소요)
kubectl rollout status deploy/longhorn-driver-deployer -n longhorn-system
kubectl get pods -n longhorn-system
```

### 2-3. Longhorn을 기본 StorageClass로 설정

```bash
# K3s 기본 StorageClass(local-path) 해제
kubectl patch storageclass local-path \
  -p '{"metadata":{"annotations":{"storageclass.kubernetes.io/is-default-class":"false"}}}'

# Longhorn을 기본으로 설정
kubectl patch storageclass longhorn \
  -p '{"metadata":{"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'

# 확인
kubectl get storageclass
# longhorn   (default)   ...
```

### 2-4. Longhorn UI 접근 (선택)

```bash
# 포트포워딩으로 로컬에서 UI 접근
kubectl port-forward svc/longhorn-frontend 8000:80 -n longhorn-system
# http://localhost:8000 에서 볼륨 상태 확인
```

### Phase 2 완료 기준

```
✅ longhorn StorageClass가 (default)로 표시
✅ Longhorn UI에서 노드 2~3개 인식
```

---

## Phase 3. 네임스페이스 및 Secret 구성

### 3-1. 네임스페이스 생성

```bash
kubectl create namespace seboard
kubectl create namespace monitoring
```

### 3-2. GHCR 이미지 풀 Secret 생성

K3s가 GHCR에서 이미지를 가져오려면 인증이 필요하다.

```bash
# GitHub Personal Access Token (read:packages 권한) 발급 후
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=laon198 \
  --docker-password=<GitHub_PAT> \
  --namespace seboard
```

### 3-3. 애플리케이션 Secret 생성 (민감 정보)

```bash
kubectl create secret generic seboard-secrets \
  --namespace seboard \
  --from-literal=DB_PASSWORD='실제비밀번호' \
  --from-literal=JWT_SECRET='실제JWT시크릿' \
  --from-literal=MAIL_PASSWORD='실제메일비밀번호' \
  --from-literal=KAKAO_CLIENT_ID='실제카카오앱키' \
  --from-literal=KAKAO_CLIENT_SECRET='실제카카오시크릿' \
  --from-literal=SYSTEM_ACCOUNT_PASSWORD='실제비밀번호'

# 확인 (값은 base64 인코딩으로 보임)
kubectl get secret seboard-secrets -n seboard
```

### 3-4. ConfigMap 생성 (비민감 설정)

```bash
kubectl create configmap seboard-config \
  --namespace seboard \
  --from-literal=DB_URL='jdbc:postgresql://postgres.seboard.svc.cluster.local:5432/seboard' \
  --from-literal=DB_USERNAME='se' \
  --from-literal=REDIS_HOST='redis.seboard.svc.cluster.local' \
  --from-literal=REDIS_PORT='6379' \
  --from-literal=FRONTEND_URL='https://se-board.kumoh.ac.kr' \
  --from-literal=KAKAO_REDIRECT_URI='https://se-board.kumoh.ac.kr/login/oauth2/code/kakao' \
  --from-literal=STORAGE_ROOT_PATH='/app/files'
```

---

## Phase 4. 서비스 배포

> 아래 YAML들은 `k8s/` 디렉토리에 파일로 관리한다.

### 4-1. PostgreSQL 배포

```yaml
# k8s/postgres.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: seboard
spec:
  serviceName: postgres
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      nodeSelector:
        role: database        # PC3에 배치
      containers:
        - name: postgres
          image: postgres:15-alpine
          env:
            - name: POSTGRES_DB
              value: seboard
            - name: POSTGRES_USER
              value: se
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: seboard-secrets
                  key: DB_PASSWORD
            - name: PGDATA
              value: /var/lib/postgresql/data/pgdata
          ports:
            - containerPort: 5432
          volumeMounts:
            - name: postgres-data
              mountPath: /var/lib/postgresql/data
          readinessProbe:
            exec:
              command: ["pg_isready", "-U", "se", "-d", "seboard"]
            initialDelaySeconds: 10
            periodSeconds: 5
  volumeClaimTemplates:
    - metadata:
        name: postgres-data
      spec:
        accessModes: ["ReadWriteOnce"]
        storageClassName: longhorn
        resources:
          requests:
            storage: 20Gi
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: seboard
spec:
  selector:
    app: postgres
  ports:
    - port: 5432
      targetPort: 5432
  clusterIP: None   # Headless Service (StatefulSet 필수)
```

### 4-2. Redis 배포

```yaml
# k8s/redis.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: seboard
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
        - name: redis
          image: redis:7-alpine
          command: ["redis-server", "--appendonly", "yes"]
          ports:
            - containerPort: 6379
          volumeMounts:
            - name: redis-data
              mountPath: /data
      volumes:
        - name: redis-data
          persistentVolumeClaim:
            claimName: redis-pvc
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: redis-pvc
  namespace: seboard
spec:
  accessModes: ["ReadWriteOnce"]
  storageClassName: longhorn
  resources:
    requests:
      storage: 2Gi
---
apiVersion: v1
kind: Service
metadata:
  name: redis
  namespace: seboard
spec:
  selector:
    app: redis
  ports:
    - port: 6379
```

### 4-3. 파일 스토리지 PVC (업로드 파일용)

```yaml
# k8s/file-storage-pvc.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: file-storage-pvc
  namespace: seboard
spec:
  accessModes: ["ReadWriteMany"]   # 여러 pod에서 공유 가능
  storageClassName: longhorn
  resources:
    requests:
      storage: 50Gi
```

### 4-4. Spring Boot 백엔드 배포

```yaml
# k8s/backend.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: seboard-backend
  namespace: seboard
spec:
  replicas: 1
  selector:
    matchLabels:
      app: seboard-backend
  template:
    metadata:
      labels:
        app: seboard-backend
    spec:
      nodeSelector:
        role: app
      imagePullSecrets:
        - name: ghcr-secret
      containers:
        - name: seboard-backend
          image: ghcr.io/laon198/seboard-be:latest
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: seboard-config
            - secretRef:
                name: seboard-secrets
          volumeMounts:
            - name: file-storage
              mountPath: /app/files
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            failureThreshold: 5
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 30
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
      volumes:
        - name: file-storage
          persistentVolumeClaim:
            claimName: file-storage-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: seboard-backend
  namespace: seboard
  labels:
    app: seboard-backend          # ServiceMonitor가 이 라벨로 찾음
spec:
  selector:
    app: seboard-backend
  ports:
    - name: http
      port: 8080
      targetPort: 8080
```

### 4-5. 프론트엔드 배포

```yaml
# k8s/frontend.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: seboard-frontend
  namespace: seboard
spec:
  replicas: 1
  selector:
    matchLabels:
      app: seboard-frontend
  template:
    metadata:
      labels:
        app: seboard-frontend
    spec:
      imagePullSecrets:
        - name: ghcr-secret
      containers:
        - name: seboard-frontend
          image: ghcr.io/laon198/seboard-fe:latest
          ports:
            - containerPort: 80
          resources:
            requests:
              memory: "64Mi"
              cpu: "50m"
            limits:
              memory: "128Mi"
              cpu: "200m"
---
apiVersion: v1
kind: Service
metadata:
  name: seboard-frontend
  namespace: seboard
spec:
  selector:
    app: seboard-frontend
  ports:
    - port: 80
```

### 4-6. Ingress 설정

```yaml
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: seboard-ingress
  namespace: seboard
  annotations:
    nginx.ingress.kubernetes.io/proxy-body-size: "30m"        # 파일 업로드 크기
    nginx.ingress.kubernetes.io/proxy-read-timeout: "60"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "60"
spec:
  ingressClassName: nginx
  rules:
    - host: se-board.kumoh.ac.kr   # 실제 도메인으로 교체
      http:
        paths:
          - path: /v1               # API 요청 (Spring Boot의 API prefix)
            pathType: Prefix
            backend:
              service:
                name: seboard-backend
                port:
                  number: 8080
          - path: /files            # 파일 서빙
            pathType: Prefix
            backend:
              service:
                name: seboard-backend
                port:
                  number: 8080
          - path: /actuator         # Prometheus 스크랩용 (외부 차단 원하면 별도 처리)
            pathType: Prefix
            backend:
              service:
                name: seboard-backend
                port:
                  number: 8080
          - path: /                 # 나머지는 프론트엔드
            pathType: Prefix
            backend:
              service:
                name: seboard-frontend
                port:
                  number: 80
```

### 4-7. 배포 적용

```bash
kubectl apply -f k8s/postgres.yaml
# postgres pod Ready 확인 후 진행
kubectl wait --for=condition=ready pod -l app=postgres -n seboard --timeout=120s

kubectl apply -f k8s/redis.yaml
kubectl apply -f k8s/file-storage-pvc.yaml
kubectl apply -f k8s/backend.yaml
# readinessProbe 통과까지 대기 (최대 2분)
kubectl wait --for=condition=ready pod -l app=seboard-backend -n seboard --timeout=180s

kubectl apply -f k8s/frontend.yaml
kubectl apply -f k8s/ingress.yaml
```

### Phase 4 완료 기준

```
✅ kubectl get pods -n seboard → 전체 Running
✅ curl http://192.168.0.200/actuator/health → {"status":"UP"}
✅ curl http://192.168.0.200/ → 프론트엔드 HTML 반환
✅ 게시글 목록/조회/작성 기능 확인 (LAN 내부에서)
```

---

## Phase 5. 모니터링 구성 (Prometheus + Grafana)

### 5-1. kube-prometheus-stack Helm 설치

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm install kube-prometheus-stack \
  prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --set grafana.adminPassword='관리자비밀번호' \
  --set prometheus.prometheusSpec.nodeSelector.role=monitoring \
  --set grafana.nodeSelector.role=monitoring

# 설치 확인
kubectl get pods -n monitoring
```

### 5-2. Spring Boot Actuator 연동 (ServiceMonitor)

```yaml
# k8s/servicemonitor.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: seboard-backend
  namespace: monitoring
  labels:
    release: kube-prometheus-stack   # Helm 릴리스명과 일치해야 함
spec:
  namespaceSelector:
    matchNames:
      - seboard
  selector:
    matchLabels:
      app: seboard-backend           # backend Service의 라벨
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 15s
```

```bash
kubectl apply -f k8s/servicemonitor.yaml
```

### 5-3. Grafana 대시보드 임포트

```bash
# Grafana 접근 (포트포워딩)
kubectl port-forward svc/kube-prometheus-stack-grafana 3000:80 -n monitoring
# http://localhost:3000 (admin / 설정한 비밀번호)
```

임포트할 대시보드 ID:
- **JVM (Micrometer)**: 4701
- **Spring Boot 2.1+**: 12900
- **Node Exporter Full**: 1860

---

## Phase 6. DB 백업 구성

### 6-1. pg_dump CronJob

```yaml
# k8s/backup-cronjob.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: backup-pvc
  namespace: seboard
spec:
  accessModes: ["ReadWriteOnce"]
  storageClassName: longhorn
  resources:
    requests:
      storage: 30Gi
---
apiVersion: batch/v1
kind: CronJob
metadata:
  name: postgres-backup
  namespace: seboard
spec:
  schedule: "0 3 * * *"      # 매일 새벽 3시 (KST 기준 = UTC 18:00)
  concurrencyPolicy: Forbid  # 이전 job 실행 중이면 건너뜀
  jobTemplate:
    spec:
      template:
        spec:
          restartPolicy: OnFailure
          containers:
            - name: backup
              image: postgres:15-alpine
              command:
                - /bin/sh
                - -c
                - |
                  FILENAME="backup-$(date +%Y%m%d-%H%M%S).sql.gz"
                  pg_dump -h postgres.seboard.svc.cluster.local \
                    -U se seboard | gzip > /backup/$FILENAME
                  echo "백업 완료: $FILENAME"
                  # 7일 이상된 백업 삭제
                  find /backup -name "*.sql.gz" -mtime +7 -delete
                  echo "오래된 백업 정리 완료"
                  ls -lh /backup
              env:
                - name: PGPASSWORD
                  valueFrom:
                    secretKeyRef:
                      name: seboard-secrets
                      key: DB_PASSWORD
              volumeMounts:
                - name: backup-storage
                  mountPath: /backup
          volumes:
            - name: backup-storage
              persistentVolumeClaim:
                claimName: backup-pvc
```

### 6-2. 백업 복구 테스트

```bash
# 백업 파일 확인
kubectl exec -n seboard \
  $(kubectl get pod -n seboard -l app=postgres -o jsonpath='{.items[0].metadata.name}') \
  -- ls /backup

# 복구 (재해 발생 시)
kubectl exec -it -n seboard <postgres-pod> -- bash
gunzip -c /backup/backup-YYYYMMDD-HHmmss.sql.gz | \
  psql -U se seboard
```

---

## Phase 7. Cloudflare Tunnel 구성

포트포워딩 없이 외부 인터넷에서 서비스에 접근할 수 있게 한다.  
공인 IP가 바뀌어도 영향 없고, Cloudflare가 DDoS 방어와 HTTPS를 자동 처리한다.

### 7-1. Cloudflare 사전 준비

1. [Cloudflare Dashboard](https://dash.cloudflare.com) 접속
2. 도메인 추가 (없으면 Cloudflare Registrar에서 구매 또는 기존 도메인 NS 변경)
3. **Zero Trust → Networks → Tunnels → Create a tunnel** 클릭
4. Tunnel 이름 입력 (예: `seboard-tunnel`)
5. **Save tunnel** → 다음 화면에서 `Connector ID` 와 `Token` 메모

### 7-2. Cloudflare Tunnel Credentials Secret 생성

```bash
# Cloudflare Dashboard에서 발급받은 터널 credentials JSON 파일 준비
# 파일 경로: ~/.cloudflared/<tunnel-id>.json

kubectl create secret generic cloudflare-tunnel-secret \
  --namespace seboard \
  --from-file=credentials.json=~/.cloudflared/<터널ID>.json
```

### 7-3. ConfigMap (cloudflared 설정)

```yaml
# k8s/cloudflared-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: cloudflared-config
  namespace: seboard
data:
  config.yaml: |
    tunnel: <터널ID>                           # 7-1에서 메모한 Tunnel ID
    credentials-file: /etc/cloudflared/credentials.json

    ingress:
      # 메인 서비스 (Nginx Ingress의 ClusterIP 또는 NodePort로 포워딩)
      - hostname: se-board.kumoh.ac.kr
        service: http://ingress-nginx-controller.ingress-nginx.svc.cluster.local:80

      # 터미네이션 규칙 (필수)
      - service: http_status:404
```

### 7-4. cloudflared Deployment

```yaml
# k8s/cloudflared.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cloudflared
  namespace: seboard
spec:
  replicas: 1
  selector:
    matchLabels:
      app: cloudflared
  template:
    metadata:
      labels:
        app: cloudflared
    spec:
      containers:
        - name: cloudflared
          image: cloudflare/cloudflared:latest
          args:
            - tunnel
            - --config
            - /etc/cloudflared/config.yaml
            - run
          volumeMounts:
            - name: config
              mountPath: /etc/cloudflared/config.yaml
              subPath: config.yaml
            - name: credentials
              mountPath: /etc/cloudflared/credentials.json
              subPath: credentials.json
      volumes:
        - name: config
          configMap:
            name: cloudflared-config
        - name: credentials
          secret:
            secretName: cloudflare-tunnel-secret
```

### 7-5. Cloudflare DNS 설정

Cloudflare Dashboard에서 설정하거나 터널 생성 시 자동 설정된다.

```
Tunnel 설정 → Public Hostname 탭:
- Subdomain: (비워두거나 www)
- Domain: se-board.kumoh.ac.kr
- Service: HTTP → ingress-nginx-controller.ingress-nginx.svc.cluster.local:80
```

또는 직접 CNAME 레코드 추가:
```
Type: CNAME
Name: @
Target: <터널ID>.cfargotunnel.com
Proxy: 켜짐 (주황 구름)
```

### 7-6. 적용

```bash
kubectl apply -f k8s/cloudflared-config.yaml
kubectl apply -f k8s/cloudflared.yaml

# 정상 동작 확인
kubectl logs -n seboard -l app=cloudflared
# "Connection ... registered" 메시지 확인

# 외부에서 접근 테스트
curl https://se-board.kumoh.ac.kr/actuator/health
```

### Phase 7 완료 기준

```
✅ cloudflared pod → Running, 로그에 "registered" 메시지
✅ 외부 인터넷에서 https://se-board.kumoh.ac.kr 접근 가능
✅ HTTPS 자동 (Cloudflare 인증서)
✅ 포트포워딩 없이 공유기 뒤에서 동작
```

---

## Phase 8. GitHub Actions CD 완성

### 8-1. GitHub Secrets 등록

GitHub 레포 → Settings → Secrets and variables → Actions에 아래 추가:

| Secret 이름 | 설명 |
|------------|------|
| `K3S_HOST` | PC1 공인 IP (Cloudflare Tunnel 이전까지는 필요, 이후엔 LAN IP + VPN 또는 Cloudflare Access로 대체) |
| `K3S_USER` | SSH 사용자명 (예: `ubuntu`) |
| `K3S_SSH_KEY` | PC1 SSH 개인키 (`cat ~/.ssh/id_rsa`) |

### 8-2. GitHub Environment 보호 설정

- GitHub 레포 → Settings → Environments → New environment: `production`
- **Required reviewers**: 본인 계정 추가 (잘못된 main push가 자동 배포되는 것 방지)

### 8-3. 최종 CD 워크플로우 (Phase 0-5에서 작성한 내용 완성본)

```yaml
# .github/workflows/cd.yml
name: CD

on:
  push:
    branches: [main]
    paths-ignore:
      - '*.md'
      - 'k8s/**'     # k8s 설정 변경은 수동 배포

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}   # laon198/seboard-be

jobs:
  # Job 1: Docker 이미지 빌드 및 GHCR 푸시
  build-and-push:
    name: Build & Push Image
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    outputs:
      image-tag: ${{ steps.vars.outputs.sha }}

    steps:
      - uses: actions/checkout@v4

      - name: Get short SHA
        id: vars
        run: echo "sha=$(git rev-parse --short HEAD)" >> $GITHUB_OUTPUT

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Set up Buildx (캐시 활용)
        uses: docker/setup-buildx-action@v3

      - name: Build & Push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ steps.vars.outputs.sha }}
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max

  # Job 2: K3s 클러스터에 배포 (build 성공 후)
  deploy:
    name: Deploy to K3s
    runs-on: ubuntu-latest
    needs: build-and-push
    environment: production      # 승인 필요 (GitHub Environment 설정)

    steps:
      - name: Deploy via SSH
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.K3S_HOST }}
          username: ${{ secrets.K3S_USER }}
          key: ${{ secrets.K3S_SSH_KEY }}
          script: |
            # 이미지 태그 업데이트 → 롤링 업데이트 자동 시작
            kubectl set image deployment/seboard-backend \
              seboard-backend=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ needs.build-and-push.outputs.image-tag }} \
              -n seboard

            # 롤아웃 완료까지 대기 (실패 시 Actions job도 실패)
            kubectl rollout status deployment/seboard-backend \
              -n seboard --timeout=180s

            echo "배포 완료: ${{ needs.build-and-push.outputs.image-tag }}"
```

---

## 데이터 마이그레이션

> 기존 단일 서버의 PostgreSQL 데이터와 파일을 K3s 클러스터로 이전하는 절차.  
> **서비스 다운타임 발생** → 새벽 시간대 진행 권장.

### 마이그레이션 전 체크리스트

- [ ] Phase 1~6 완료 (K3s 클러스터 정상 동작 확인)
- [ ] 신규 K3s 클러스터에서 서비스 정상 동작 확인 (빈 DB로)
- [ ] 현재 서버 DB 백업 (마이그레이션 당일 fresh 백업)
- [ ] 파일 업로드 디렉토리 크기 확인

### Step 1. 기존 서버 DB 덤프

```bash
# 기존 서버에서 실행
pg_dump -U postgres -d se_new \
  --no-owner --no-acl \
  -f /tmp/seboard-migrate-$(date +%Y%m%d).sql

# 덤프 파일을 로컬로 복사
scp ubuntu@기존서버IP:/tmp/seboard-migrate-*.sql ./
```

### Step 2. K3s PostgreSQL로 복구

```bash
# postgres pod 이름 확인
PG_POD=$(kubectl get pod -n seboard -l app=postgres -o jsonpath='{.items[0].metadata.name}')

# 덤프 파일 pod으로 복사
kubectl cp ./seboard-migrate-YYYYMMDD.sql seboard/$PG_POD:/tmp/

# 복구 실행
kubectl exec -n seboard $PG_POD -- \
  psql -U se -d seboard -f /tmp/seboard-migrate-YYYYMMDD.sql

# 데이터 확인
kubectl exec -n seboard $PG_POD -- \
  psql -U se -d seboard -c "SELECT COUNT(*) FROM post;"
```

### Step 3. 업로드 파일 이전

```bash
# 기존 서버에서 파일 압축
ssh ubuntu@기존서버IP \
  "tar czf /tmp/files-backup.tar.gz -C /path/to/files ."

# 로컬로 복사
scp ubuntu@기존서버IP:/tmp/files-backup.tar.gz ./

# K3s file-storage-pvc에 복사
# 임시 pod 생성하여 파일 복사
kubectl run file-copy --image=alpine --restart=Never \
  -n seboard \
  --overrides='{"spec":{"volumes":[{"name":"fs","persistentVolumeClaim":{"claimName":"file-storage-pvc"}}],"containers":[{"name":"file-copy","image":"alpine","command":["sleep","3600"],"volumeMounts":[{"name":"fs","mountPath":"/app/files"}]}]}}'

kubectl cp ./files-backup.tar.gz seboard/file-copy:/tmp/
kubectl exec -n seboard file-copy -- \
  tar xzf /tmp/files-backup.tar.gz -C /app/files

# 완료 후 임시 pod 삭제
kubectl delete pod file-copy -n seboard
```

### Step 4. DNS 절체 (트래픽 전환)

```
1. Cloudflare Tunnel이 정상 동작 중인지 확인
2. 기존 서버의 서비스 중단 (nginx stop 또는 Spring Boot 종료)
3. 기존 DNS 레코드 삭제 (또는 TTL 0으로 설정)
4. Cloudflare에서 새 도메인이 K3s로 향하는지 확인
5. https://se-board.kumoh.ac.kr 에서 기능 점검
```

---

## 롤백 계획

문제 발생 시 기존 단일 서버로 즉시 복귀하는 절차.

### 롤백 조건

- K3s 마이그레이션 후 주요 기능 장애 발생
- 성능 저하 심각 (기존 서버 대비)
- 데이터 무결성 문제 발견

### 롤백 절차

```bash
# 1. 기존 서버 Spring Boot 재기동
ssh ubuntu@기존서버IP "sudo systemctl start seboard"

# 2. DNS를 기존 서버 IP로 되돌림
# Cloudflare Dashboard → DNS → 기존 A 레코드로 변경

# 3. 마이그레이션 이후 생성된 데이터 확인
# (K3s에만 있는 데이터가 있으면 기존 서버 DB에 수동 반영)
```

### K8s 배포 롤백 (코드 문제)

```bash
# 이전 버전으로 즉시 롤백
kubectl rollout undo deployment/seboard-backend -n seboard

# 특정 버전으로 롤백
kubectl rollout history deployment/seboard-backend -n seboard
kubectl rollout undo deployment/seboard-backend --to-revision=2 -n seboard
```

---

## 체크리스트

### Phase 0 - 사전 준비
- [ ] `application-prod.yml` 작성 (환경변수 참조)
- [ ] `.gitignore` 수정 (application-prod.yml 허용)
- [ ] 백엔드 `Dockerfile` 멀티스테이지 빌드 작성
- [ ] 로컬 `docker run`으로 Spring Boot 정상 기동 확인
- [ ] 프론트엔드 `Dockerfile` + `nginx.conf` 작성
- [ ] GitHub Actions CI 워크플로우 (자동 테스트)
- [ ] GitHub Actions CD 워크플로우 (이미지 빌드·푸시)
- [ ] GHCR에 이미지 정상 푸시 확인

### Phase 1 - K3s 구성
- [ ] 모든 노드 Ubuntu 22.04 설치
- [ ] swap 비활성화 (전 노드)
- [ ] 방화벽 포트 설정 (전 노드)
- [ ] 고정 IP 설정 (전 노드)
- [ ] 마스터 노드(PC1) K3s 설치
- [ ] 워커 노드(PC2, PC3) K3s 조인
- [ ] `kubectl get nodes` → 3개 모두 Ready
- [ ] Nginx Ingress 설치
- [ ] MetalLB 설치 및 IP 풀 설정
- [ ] Ingress에 EXTERNAL-IP 할당 확인

### Phase 2 - 스토리지
- [ ] open-iscsi 설치 (전 노드)
- [ ] Longhorn Helm 설치
- [ ] Longhorn을 default StorageClass로 설정

### Phase 3 - Secret/Config
- [ ] `seboard` 네임스페이스 생성
- [ ] GHCR pull secret 생성
- [ ] `seboard-secrets` Secret 생성
- [ ] `seboard-config` ConfigMap 생성

### Phase 4 - 서비스 배포
- [ ] PostgreSQL StatefulSet 배포 및 Ready 확인
- [ ] Redis Deployment 배포
- [ ] file-storage-pvc 생성
- [ ] Spring Boot Deployment 배포
- [ ] readinessProbe 통과 확인
- [ ] Frontend Deployment 배포
- [ ] Ingress 설정 적용
- [ ] LAN 내부에서 서비스 동작 확인

### Phase 5 - 모니터링
- [ ] kube-prometheus-stack Helm 설치
- [ ] ServiceMonitor 설정
- [ ] Prometheus에서 seboard-backend 타겟 확인
- [ ] Grafana 대시보드 임포트 (4701, 12900, 1860)

### Phase 6 - 백업
- [ ] backup-pvc 생성
- [ ] pg_dump CronJob 배포
- [ ] 수동 Job 실행으로 백업 정상 생성 확인
- [ ] 복구 테스트 (스테이징 DB에서)

### Phase 7 - Cloudflare Tunnel
- [ ] Cloudflare 계정 및 도메인 준비
- [ ] Tunnel 생성 및 credentials.json 다운로드
- [ ] cloudflare-tunnel-secret Secret 생성
- [ ] cloudflared-config ConfigMap 생성
- [ ] cloudflared Deployment 배포
- [ ] 외부에서 HTTPS 접근 확인

### Phase 8 - CD 자동화
- [ ] GitHub Secrets 등록 (K3S_HOST, K3S_USER, K3S_SSH_KEY)
- [ ] GitHub Environment `production` 생성
- [ ] CD 워크플로우 main push 테스트
- [ ] 자동 롤아웃 완료 확인

### 데이터 마이그레이션
- [ ] 기존 DB pg_dump
- [ ] K3s PostgreSQL로 복구
- [ ] 데이터 건수 검증
- [ ] 업로드 파일 이전
- [ ] DNS 절체
- [ ] 전체 기능 점검

---

## 예상 소요 시간

| Phase | 작업 | 예상 시간 |
|-------|------|----------|
| 0 | 사전 준비 (환경변수, Dockerfile, CI/CD) | 1~2일 |
| 1 | K3s 클러스터 구성 | 반나절~1일 |
| 2 | Longhorn 스토리지 | 2~3시간 |
| 3 | Secret/Config | 1시간 |
| 4 | 서비스 배포 및 검증 | 반나절~1일 |
| 5 | 모니터링 | 반나절 |
| 6 | 백업 구성 | 2~3시간 |
| 7 | Cloudflare Tunnel | 2~3시간 |
| 8 | CD 자동화 완성 | 2~3시간 |
| 데이터 이전 | 마이그레이션 | 2~4시간 |
| **전체** | | **5~8일** |

> AI 활용 시 삽질 시간 포함한 현실적인 예상치
