# DEV-ONLY RSA KEYPAIR — DO NOT USE IN PRODUCTION

`privateKey.pem` / `publicKey.pem` are a throwaway RSA-2048 pair generated with:

```
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out privateKey.pem
openssl rsa -pubout -in privateKey.pem -out publicKey.pem
```

They are committed **only** so that `dev` and `test` are self-contained: the module can sign
tokens and verify its own tokens without any external authorization server.

The private key is public in this repository — **any token it signs is forgeable by anyone.**

In production, override both locations with real, secret material:

- `JWT_PRIVATE_KEY_LOCATION` (default `jwt/privateKey.pem`) — signing key
- `JWT_PUBLIC_KEY_LOCATION` (default `jwt/publicKey.pem`) — verification key, also published
  through `GET /uaa/.well-known/jwks.json`
