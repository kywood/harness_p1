# 일회용 ffmpeg 이미지 (공식 Debian 이미지 + Debian 저장소의 ffmpeg). 작업 후 docker rmi pieceplay-ffmpeg 로 지운다.
FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*
WORKDIR /w
ENTRYPOINT ["ffmpeg"]
