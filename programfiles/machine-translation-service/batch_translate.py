#!/usr/bin/python3
import sys
from transformers import MarianTokenizer, MarianMTModel
from typing import List
import torch

#device = "cpu"
device = "cuda:0"

dir=sys.argv[1] + "machine-translation-service"
src = 'en'  # source language
trg = 'ar'  # target language
sample_text = ">>ara<< This is an example of a revolution."
mname = f'{dir}/data/opus-mt-{src}-{trg}'

tokenizer = MarianTokenizer.from_pretrained(mname)
model = MarianMTModel.from_pretrained(mname)
model = model.to(device)

lines = []
while True:
    for line in sys.stdin:
        line = line.strip()
        if line == 'EOD':
            inputs    = tokenizer(lines, return_tensors="pt", padding=True).to(device)
            translated   = model.generate(**inputs).to(device)
            [print(tokenizer.decode(t, skip_special_tokens=True)) for t in translated]
            print('EOL')
            sys.stdout.flush()
            lines.clear()
        elif line.startswith('EOF'):
            sys.exit(0)
        else:
            lines.append(line)
sys.exit(0)