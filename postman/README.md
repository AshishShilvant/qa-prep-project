# Postman collection

Drop your exported files here so CI can find them:

- `collection.json` — exported Postman collection
- `env.json` — exported Postman environment

The CI workflow (`.github/workflows/ci.yml`) runs:

```
newman run postman/collection.json -e postman/env.json
```

until both files exist, that step will fail.
