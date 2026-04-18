# Docker Compose Setup with Colima

## Issue: "operation not supported" when starting docker-compose

When you get the error:
```
Error response from daemon: error while creating mount source path '/Users/saha/.colima/default/docker.sock': 
mkdir /Users/saha/.colima/default/docker.sock: operation not supported
```

This means **Colima is not running**. When Colima is not running, the socket file doesn't exist, and Docker tries to create it as a directory, which fails.

## Solution

### Step 1: Start Colima First

```bash
colima start
```

Verify it's running:
```bash
colima status
```

### Step 2: Start Docker Compose

Once Colima is running and the socket exists at `~/.colima/default/docker.sock`, start the services:

```bash
docker-compose up -d
```

### Automated: Use the Startup Script

The easiest way is to use the provided script which checks Colima status automatically:

```bash
chmod +x start-colima.sh
./start-colima.sh
```

This script will:
1. Check if Colima is running (start it if needed)
2. Verify the Docker socket exists
3. Start docker-compose services
4. Display access URLs

## How It Works

### Why Docker Socket Access?

- **Alloy** (the monitoring agent) needs to discover Docker containers running on your system
- Since Colima runs a lightweight VM with Docker, it exposes the Docker socket at: `~/.colima/default/docker.sock`
- When docker-compose mounts this socket into the Alloy container at `/var/run/docker.sock`, Alloy can see and monitor containers

### Docker Compose Configuration

```yaml
alloy:
  volumes:
    - /var/run/docker.sock:/var/run/docker.sock:ro
```

The key insight:
``- **On your host**: The socket is at `~/.colima/default/docker.sock`
- **Colima VM**: Exposes it at `/var/run/docker.sock` inside containers
``- **In Alloy container**: Accessible at `/var/run/docker.sock`

This is why we mount `/var/run/docker.sock` — it's Colima's internal path that's already exposed.

## Verification

After starting services with the script (which includes a wait), verify Alloy can discover containers:

1. Open Alloy UI: http://localhost:12345
2. Go to **Targets** section
3. Check **discovery.docker.containers**
4. You should see running containers listed (not empty [])

## Troubleshooting

### If targets are still empty:

```bash
# Check Colima status
colima status

# Verify socket exists
ls -la ~/.colima/default/docker.sock

# Check Alloy logs
docker-compose logs alloy
```

### If you get permission errors:

Colima automatically handles permissions, but if needed:
```bash
# Check Docker socket permissions
ls -la ~/.colima/default/docker.sock
# Should look like: srw-rw-rw-@ ... docker.sock
```

### Stop everything:

```bash
docker-compose down
colima stop
```

## Quick Reference

```bash
# Start everything
./start-colima.sh

# Or manually:
colima start
docker-compose up -d

# View logs
docker-compose logs -f alloy

# Stop everything
docker-compose down
colima stop

# Access points
# Grafana: http://localhost:3000 (admin/admin)
# Alloy: http://localhost:12345
# Loki: http://localhost:3100
```

## Access Points

- **Grafana**: http://localhost:3000 
  - Username: `admin`
  - Password: `admin`
- **Alloy UI**: http://localhost:12345
- **Loki API**: http://localhost:3100

