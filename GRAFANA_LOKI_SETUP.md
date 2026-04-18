# How to Connect Loki to Grafana & View Logs

## Step 1: Add Loki as a Data Source in Grafana

1. Open Grafana: http://localhost:3000
2. Login with `admin` / `admin`
3. Click the **gear icon** (Settings) → **Data sources**
4. Click **"Add data source"**
5. Search for and select **"Loki"**
6. Configure with these settings:

### Data Source Configuration

**Name:** `Loki` (or any name you prefer)

**URL:** `http://localhost:3100`

⚠️ **IMPORTANT:** Make sure the URL is correct:
- From browser/Grafana container perspective: `http://localhost:3100`
- Do NOT use `http://loki-1:3100` unless both are on the same docker network
- Since Loki is on the bridge network and Grafana is also on bridge, use `http://loki:3100` might work, but `http://localhost:3100` should work since both expose ports

**Authentication:** Leave as "No auth"

**Click "Save & test"**

You should see: ✅ **Data source is working**

---

## Step 2: Create a Dashboard to View Logs

### Method 1: Quick Explore (Easiest)

1. Click the **Compass icon** on the left (Explore)
2. Select **Loki** data source (top left)
3. In the query builder, click **"Select a log stream"**
4. Select any label you want to filter by, for example:
   - `container_name` = `distracted_diffie`
   - `job` = `docker-services`
5. Click the **blue play button** to execute the query
6. You'll see the logs appear below!

### Method 2: Create a Dashboard with Logs Panel

1. Go to **Dashboards** → **New dashboard**
2. Click **"Add panel"**
3. Select **Loki** as the data source
4. In the query editor, enter one of these queries:

**Show all logs:**
```
{job="docker-services"}
```

**Show logs from specific container:**
```
{container_name="distracted_diffie"}
```

**Show logs with specific level:**
```
{job="docker-services"} | json | level="error"
```

5. Click **Run query** or press `Ctrl+Enter`
6. Click **"Save"** to save the dashboard

---

## Common Query Examples

### Query 1: All Docker Service Logs
```
{job="docker-services"}
```

### Query 2: Specific Container Logs
```
{container_name="engine-alloy-1"}
```

### Query 3: Multiple Containers
```
{container_name=~"engine-.*"}
```

### Query 4: Logs with JSON Parsing
```
{job="docker-services"} | json
```

### Query 5: Filter by Log Content
```
{job="docker-services"} |= "error"
```

---

## Troubleshooting

### Issue: "No data source found"
- Make sure you added Loki as a data source in Settings → Data sources
- Check the URL is `http://localhost:3100`
- Click "Save & test" to verify the connection

### Issue: "No data" returned in query
1. Check that containers are discovered:
   ```bash
   curl -s "http://localhost:3100/loki/api/v1/label/container_name/values" | python3 -m json.tool
   ```
   You should see: `["distracted_diffie", "engine-alloy-1", "engine-grafana-1", "engine-loki-1"]`

2. Verify logs exist:
   ```bash
   curl -s "http://localhost:3100/loki/api/v1/labels" | python3 -m json.tool
   ```
   You should see: `["container_name", "job", "service_name"]`

### Issue: Grafana can't reach Loki
- Check if Loki is running: `docker ps | grep loki`
- Check if port 3100 is exposed: `docker ps -p 3100`
- Try the URL from terminal: `curl http://localhost:3100/ready`
- If using docker compose networks, use `http://loki:3100` instead (if both on same network)

### Issue: Logs are not being collected
1. Check Alloy logs:
   ```bash
   docker-compose logs alloy | grep -i "docker\|error" | tail -20
   ```

2. Verify containers are being discovered:
   ```bash
   curl -s "http://localhost:3100/loki/api/v1/label/container_name/values"
   ```

3. Restart docker-compose:
   ```bash
   docker-compose down && sleep 2 && docker-compose up -d
   ```

---

## Working Example

Here's what you should see in Grafana after querying logs:

**Query:** `{job="docker-services"}`

**Results:**
```
container_name: distracted_diffie
job: docker-services
service_name: distracted_diffie

Server running on http://localhost:3000/
> node index.js
> node-hello@1.0.0 start
found 0 vulnerabilities
up to date, audited 1 package in 299ms
```

---

## Quick Checklist

- [ ] Loki is running: `docker ps | grep loki`
- [ ] Loki is accessible: `curl http://localhost:3100/ready`
- [ ] Grafana is running: `docker ps | grep grafana`
- [ ] Data source added in Grafana settings
- [ ] Data source test passed (✅ Data source is working)
- [ ] Containers are discovered: `curl http://localhost:3100/loki/api/v1/labels`
- [ ] Query returns data: Try `{job="docker-services"}` in Explore

If all checkboxes are checked, you should see logs in Grafana!

