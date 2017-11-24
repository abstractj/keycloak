# Instructions to generate a new client certificate
```
## Create a certificate signing request (CSR)
openssl req -new -config client.cnf -key client.key -out client.csr
```
## Request a client certificate
```
openssl x509 -req -in client.csr -CA ocsp/certs/intermediate-ca.crt -CAkey ocsp/private/intermediate-ca.key -CAcreateserial -out client.crt -days 50950 -extensions v3_req -extfile client.cnf
```

## Export to PKCS12
```
openssl pkcs12 -export -passin pass:secret -password pass:secret -in client.crt -inkey client.key -out client.p12
```

## Java Key Convert PKCS12 keystore to JKS keytstore
```
keytool -importkeystore -destkeystore client.jks -deststorepass secret -srckeystore client.p12 -srcstoretype PKCS12 -srcstorepass secret
```
