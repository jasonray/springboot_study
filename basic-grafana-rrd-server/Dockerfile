# FROM golang:1.16-alpine AS builder
# FROM blang/golang-alpine AS builder
FROM golang:alpine3.9 as builder

RUN apk update && apk add pkgconfig rrdtool-dev gcc libc-dev git

WORKDIR /build
COPY . .
RUN go build -o grafana-rrd-server

FROM alpine:3.9
RUN apk add rrdtool rrdtool-dev
COPY --from=builder /build/grafana-rrd-server /grafana-rrd-server
WORKDIR /
ENTRYPOINT [ "/grafana-rrd-server" ]
