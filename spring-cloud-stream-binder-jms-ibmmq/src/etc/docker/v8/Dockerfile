FROM ibmcom/mq:8

RUN useradd mq -G mqm && echo mq:mq | chpasswd

COPY config.mqsc /etc/mqm/
