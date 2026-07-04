# ====================================================================
#  校园失物招领 AI 增强服务 Dockerfile
#  说明：模型在首次请求时懒加载，镜像构建时不下载模型，保持镜像精简
# ====================================================================
FROM python:3.10-slim

# 基础系统依赖：Pillow 解码图片需要 libjpeg/zlib 等
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 先装依赖，利用 Docker 层缓存
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 再拷贝业务代码
COPY . .

EXPOSE 8001

# 通过 uvicorn 启动，监听 8001
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]
