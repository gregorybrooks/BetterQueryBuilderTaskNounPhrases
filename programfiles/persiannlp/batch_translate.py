#!/usr/bin/python3
import sys
#from transformers import MarianTokenizer, MarianMTModel
from transformers import MT5ForConditionalGeneration, MT5Tokenizer
from typing import List
import torch

#device = "cpu"
device = "cuda:0"

dir=sys.argv[1] + "persiannlp"
size="base"
mname = f'{dir}/data/mt5-{size}-parsinlu-translation_en_fa'

#tokenizer = MarianTokenizer.from_pretrained(mname)
#model = MarianMTModel.from_pretrained(mname)
tokenizer = MT5Tokenizer.from_pretrained(mname)
model = MT5ForConditionalGeneration.from_pretrained(mname)
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
