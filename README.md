# Minecraft Server Architecture

Minecraft server setup for a homelab environment. Includes network segmentation, service configuration, and a log processing pipeline.

## Log Processing

The log pipeline utilizes `vector` and exists to:

- Capture Minecraft server events in real time
- Extract meaningful signals (e.g. player deaths)
- Filter noise
- Forward structured events to external systems (Discord webhook)

### How to Run

Vector behavior is controlled by environment variables

__Required__

- `VECTOR_DATA_DIR`
  - Directory where Vector stores checkpoints/state.
- `MINECRAFT_LOG_PATH`
  - Path to `logs/latest.log`.

__Optional__

- `READ_FROM` (default: `end`)
  - `end` → production mode, no replay
  - `beginning` → replay entire log (debug/testing)
- `DEBUG` (default: `true`)
  - `true` → disables Discord sink
  - `false` → enables Discord notifications
- `DISCORD_WEBHOOK_URI`
  - Required only when `DEBUG=false`.

Example

```bash
export VECTOR_DATA_DIR=/var/lib/vector
export MINECRAFT_LOG_PATH=/srv/minecraft/logs/latest.log
export READ_FROM=end
export DEBUG=false
export DISCORD_WEBHOOK_URI=https://discord.com/api/webhooks/...

vector -c ./vector/vector.toml
```

## Running as a `systemd` Service

Run Vector as a long-lived service on the server host using `systemd`

Create the unit file:

```bash
sudo vim /etc/systemd/system/vector-minecraft.service
```

```ini
[Unit]
Description=Vector log agent for Minecraft
After=network.target

[Service]
Type=simple
User=<user>
Group=<group>
WorkingDirectory=/path/to/this_repo
EnvironmentFile=/path/to/.vector.env
ExecStart=/usr/bin/vector -c /path/to/vector/vector.toml
Restart=always
RestartSec=5s
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

Then:

```bash
sudo systemctl daemon-reload
sudo systemctl enable vector-minecraft
sudo systemctl start vector-minecraft
```

Check status:

```bash
sudo systemctl status vector-minecraft
```

The console sink allows inspection via:

```bash
journalctl -u vector-minecraft -f
```

