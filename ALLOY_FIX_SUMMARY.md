# ✅ Alloy Container Discovery - FIXED

## Summary

The issue was that **Alloy could not discover Docker containers when running as a docker-compose service** on Colima with the default network configuration. The solution required:

1. **Using `network_mode: host`** - This allows Alloy to access the host's Docker socket directly
2. **Mounting the Docker socket** - Making `/var/run/docker.sock` available inside the Alloy container
3. **Updating config.alloy** - Ensuring the correct socket path is used

## Changes Made

### 1. docker-compose.yaml
```yaml
alloy:
  network_mode: host  # ← KEY CHANGE: Use host network
  volumes:
    - ./config.alloy:/etc/alloy/config.alloy:ro
    - /var/run/docker.sock:/var/run/docker.sock:ro  # ← Mount Docker socket
```

### 2. config.alloy
Updated discovery and log source to use the mounted Docker socket:
```
discovery.docker "containers" {
  host = "unix:///var/run/docker.sock"  # ← Use mounted socket path
  refresh_interval = "15s"
}

loki.source.docker "docker_logs" {
  host = "unix:///var/run/docker.sock"  # ← Use mounted socket path
  targets = discovery.relabel.docker_logs.output
  forward_to = [loki.process.log_refiner.receiver]
}
```

## Verification

### Containers Being Discovered
```bash
curl -s "http://localhost:3100/loki/api/v1/label/container_name/values" | python3 -m json.tool

# Output:
{
    "status": "success",
    "data": [
        "distracted_diffie",
        "engine-alloy-1",
        "engine-grafana-1",
        "engine-loki-1"
    ]
}
```

### Labels Available in Loki
```bash
curl -s "http://localhost:3100/loki/api/v1/labels" | python3 -m json.tool

# Output:
{
    "status": "success",
    "data": [
        "container_name",
        "job",
        "service_name"
    ]
}
```

### Jobs Being Collected
```bash
curl -s "http://localhost:3100/loki/api/v1/label/job/values" | python3 -m json.tool

# Output:
{
    "status": "success",
    "data": [
        "docker-services"
    ]
}
```

## Why This Works on Colima

When you run `docker-compose up` with Colima:

1. **Colima VM** runs Docker daemon and exposes the socket at `/var/run/docker.sock` inside the VM
2. **Alloy with host network** bypasses the docker-compose network bridge and connects directly to the host
3. **Socket mount** makes the Docker daemon accessible to Alloy
4. **Alloy discovers all containers** running in the Colima VM and streams their logs to Loki

## Access Points

- **Grafana**: http://localhost:3000 (admin/admin)
- **Alloy UI**: http://localhost:12345
- **Loki**: http://localhost:3100
- **Your App**: http://localhost:9479 (node container)

## Commands

```bash
# Check if services are running
docker ps

# View logs
docker-compose logs -f alloy

# Query logs in Loki
curl -s "http://localhost:3100/loki/api/v1/label/container_name/values" | python3 -m json.tool

# Stop everything
docker-compose down
colima stop
```

## Key Takeaway

**Host network mode is essential** for docker-compose services that need to interact with the host's Docker daemon on Colima. This is different from standard Docker Desktop where containers can access the socket through the docker-compose network bridge.

