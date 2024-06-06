from flask import Flask, request, jsonify
import torch
import numpy as np
import cv2
from PIL import Image
import urllib.request
from urllib.parse import unquote
from io import BytesIO
import mysql.connector
from dotenv import load_dotenv
import os
import ssl
import logging
import pandas as pd
import json

app = Flask(__name__)

dotenv_path = "resources/application-SECRET-KEY.properties"
load_dotenv(dotenv_path)

# SSL 검증 비활성화
ssl._create_default_https_context = ssl._create_unverified_context

# 라벨 매핑 딕셔너리
label_mapping = {
    'bag': '가방', 'jewelry': '귀금속', 'others': '기타물품', 'books': '도서용품',
    'documents': '서류', 'shoppingbag': '쇼핑백', 'sportsEquipment': '스포츠용품',
    'instrument': '악기', 'securities': '유가증권', 'clothing': '의류', 'car': '자동차',
    'electronics': '전자기기', 'certificate': '증명서', 'wallet': '지갑', 'card': '카드',
    'computer': '컴퓨터', 'cash': '현금', 'phone': '휴대폰'
}

def load_image_from_url(url):
    try:
        decoded_url = unquote(url)
        resp = urllib.request.urlopen(decoded_url)
        image = np.asarray(bytearray(resp.read()), dtype='uint8')
        image = cv2.imdecode(image, cv2.IMREAD_COLOR)
        return image
    except Exception as e:
        logging.error(f"Error loading the image from URL: {e}")
        return None

@app.route('/process_image', methods=['POST'])
def process_image():
    try:
        data = request.json
        input_image_url = data.get('image_url')

        if not input_image_url:
            return jsonify({'error': 'No image URL provided'}), 400

        model_path = 'models/best.pt'

        try:
            model = torch.hub.load('ultralytics/yolov5', 'custom', path=model_path)
        except Exception as e:
            logging.error(f"Model loading failed: {e}")
            return jsonify({'error': f"Model loading failed: {e}"}), 500

        input_image = load_image_from_url(input_image_url)

        if input_image is None:
            return jsonify({'error': 'Failed to load the input image'}), 400

        open_cv_image = cv2.cvtColor(np.array(input_image), cv2.COLOR_RGB2BGR)
        results = model(open_cv_image)

        df = results.pandas().xyxy[0]
        df['area'] = (df['xmax'] - df['xmin']) * (df['ymax'] - df['ymin'])
        largest_label = df.loc[df['area'].idxmax()]['name']
        translated_label = label_mapping.get(largest_label, largest_label)

        # UTF-8로 변환하여 응답
        translated_label = translated_label.encode('utf-8').decode('utf-8')
        response_data = {'translated_label': translated_label}
        response = json.dumps(response_data, ensure_ascii=False)
        return response, 200

    except Exception as e:
        logging.error(f"An error occurred: {e}")
        return jsonify({'error': f"An error occurred: {e}"}), 500

if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    app.run(debug=True)
