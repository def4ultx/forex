# Proxy for Forex rate

## Getting started

Build and publish docker image and run it

```bash
sbt docker:publishLocal
docker-compose up
```

Get exchange rate with example request

```bash
curl localhost:9090/rates?from=USD&to=JPY
```
