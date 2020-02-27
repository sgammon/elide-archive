FROM envoyproxy/envoy-alpine:v1.13.0
COPY envoy.yaml /etc/envoy/envoy.yaml
COPY tls.key /etc/ssl/tls.key
COPY tls.crt /etc/ssl/tls.crt
EXPOSE 443
EXPOSE 8090
EXPOSE 9901