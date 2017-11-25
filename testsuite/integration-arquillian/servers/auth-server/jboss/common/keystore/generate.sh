
# FIXME
openssl req -new -config client.cnf -key client.key -out client.csr

openssl x509 -req -in client.csr -CA ocsp/certs/intermediate-ca.crt -CAkey ocsp/private/intermediate-ca.key -CAcreateserial -out client.crt -days 50950 -extensions usr_cert -extfile client.cnf

cat ca.crt ocsp/certs/intermediate-ca.crt ocsp/certs/ca-chain.crt client.crt > new-client.crt
mv new-client.crt client.crt

openssl pkcs12 -export -passin pass:secret -password pass:secret -in client.crt -inkey client.key -out client.p12
#
keytool -importkeystore -destkeystore client.jks -deststorepass secret -srckeystore client.p12 -srcstoretype PKCS12 -srcstorepass secret
