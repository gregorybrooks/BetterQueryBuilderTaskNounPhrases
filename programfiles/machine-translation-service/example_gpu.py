from transformers import MarianTokenizer, MarianMTModel
from typing import List
import torch

device = "cpu"
#device = "cuda:0" if torch.cuda.is_available() else "cpu"

src = 'en'  # source language
trg = 'ar'  # target language
mname = f'data/opus-mt-{src}-{trg}'
example_doc = [f'tweet\nEthiopia\u2019s Prime Minister Hailemariam Desalegn said on Thursday he had submitted his resignation as both premier and the chairman of the ruling coalition in an effort to facilitate reforms following a period of mass unrest.',f'\nCAPE TOWNSOUTH AFRICA, 06MAY11 \u2013 Hailemariam Desalegn, Deputy Prime Minister of Ethiopia and Minister of Foreign Affiars of Ethiopia, during the announcement that the 2012 World Economic Forum on Africa will be held in Ethiopia at the World Economic Forum on Africa 2011 held in Cape Town, South Africa, 4-6 May 2011.',f'\nCopyright (cc-by-sa) © World Economic Forum ( www.',f'weforum.',f'org \/Photo Matthew Jordaan [email protected]\nHundreds of people have died in violence sparked initially by an urban development plan for the capital Addis Ababa.',f' The unrest spread in 2015 and 2016 as demonstrations against political restrictions and human rights abuses broke out.',f'\n Unrest and a political crisis have led to the loss of lives and displacement of many,  Hailemariam said in a televised address to the nation.',f'\nAdvertisement\n \u2026 I see my resignation as vital in the bid to carry out reforms that would lead to sustainable peace and democracy,  he said.',f'\nHailemariam said he would stay on as prime minister in a caretaker capacity until the ruling Ethiopian People\u2019s Revolutionary Democratic Front (EPRDF) and the country\u2019s parliament accepted his resignation and named a new premier.',f'\nReporting by Aaron Maasho; Editing by Gareth Jones\nAdvertisement\n',
f'tweet\nEthiopia\u2019s Prime Minister Hailemariam Desalegn said on Thursday he had submitted his resignation as both premier and the chairman of the ruling coalition in an effort to facilitate reforms following a period of mass unrest.',f'\nCAPE TOWNSOUTH AFRICA, 06MAY11 \u2013 Hailemariam Desalegn, Deputy Prime Minister of Ethiopia and Minister of Foreign Affiars of Ethiopia, during the announcement that the 2012 World Economic Forum on Africa will be held in Ethiopia at the World Economic Forum on Africa 2011 held in Cape Town, South Africa, 4-6 May 2011.',f'\nCopyright (cc-by-sa) © World Economic Forum ( www.',f'weforum.',f'org \/Photo Matthew Jordaan [email protected]\nHundreds of people have died in violence sparked initially by an urban development plan for the capital Addis Ababa.',f' The unrest spread in 2015 and 2016 as demonstrations against political restrictions and human rights abuses broke out.',f'\n Unrest and a political crisis have led to the loss of lives and displacement of many,  Hailemariam said in a televised address to the nation.',f'\nAdvertisement\n \u2026 I see my resignation as vital in the bid to carry out reforms that would lead to sustainable peace and democracy,  he said.',f'\nHailemariam said he would stay on as prime minister in a caretaker capacity until the ruling Ethiopian People\u2019s Revolutionary Democratic Front (EPRDF) and the country\u2019s parliament accepted his resignation and named a new premier.',f'\nReporting by Aaron Maasho; Editing by Gareth Jones\nAdvertisement\n',
f'tweet\nEthiopia\u2019s Prime Minister Hailemariam Desalegn said on Thursday he had submitted his resignation as both premier and the chairman of the ruling coalition in an effort to facilitate reforms following a period of mass unrest.',f'\nCAPE TOWNSOUTH AFRICA, 06MAY11 \u2013 Hailemariam Desalegn, Deputy Prime Minister of Ethiopia and Minister of Foreign Affiars of Ethiopia, during the announcement that the 2012 World Economic Forum on Africa will be held in Ethiopia at the World Economic Forum on Africa 2011 held in Cape Town, South Africa, 4-6 May 2011.',f'\nCopyright (cc-by-sa) © World Economic Forum ( www.',f'weforum.',f'org \/Photo Matthew Jordaan [email protected]\nHundreds of people have died in violence sparked initially by an urban development plan for the capital Addis Ababa.',f' The unrest spread in 2015 and 2016 as demonstrations against political restrictions and human rights abuses broke out.',f'\n Unrest and a political crisis have led to the loss of lives and displacement of many,  Hailemariam said in a televised address to the nation.',f'\nAdvertisement\n \u2026 I see my resignation as vital in the bid to carry out reforms that would lead to sustainable peace and democracy,  he said.',f'\nHailemariam said he would stay on as prime minister in a caretaker capacity until the ruling Ethiopian People\u2019s Revolutionary Democratic Front (EPRDF) and the country\u2019s parliament accepted his resignation and named a new premier.',f'\nReporting by Aaron Maasho; Editing by Gareth Jones\nAdvertisement\n']
print('Size of doc: ' + str(len(example_doc)))

print('Getting tokenizer')
tokenizer = MarianTokenizer.from_pretrained(mname)
#print(tokenizer.supported_language_codes)

print('Getting model')
model = MarianMTModel.from_pretrained(mname)
model     = model.to(device)

print('getting inputs')
inputs    = tokenizer(example_doc, return_tensors="pt", padding=True).to(device)
print('generating')
translated   = model.generate(**inputs)
print('decoding')
[print(tokenizer.decode(t, skip_special_tokens=True)) for t in translated]

print('do it again')
inputs    = tokenizer(example_doc, return_tensors="pt", padding=True).to(device)
translated   = model.generate(**inputs)
[print(tokenizer.decode(t, skip_special_tokens=True)) for t in translated]

print('do it again')
example_doc = f'East Africa\nEthiopia ends online blackout\nMobile and broadband internet services shut down in December in many regions outside the capital that were hit by unrest that threatened the ruling coalition\u2019s tight hold on country.\nBy\nWhatsApp\nADDIS ABABA, April 16 (Reuters) \u2013 Internet users in Ethiopia said on Monday the government appeared to have ended a three-month online blackout, raising hopes of a relaxation of restrictions after the arrival of a new prime minister who promised reforms.\nMobile and broadband internet services shut down in December in many regions outside the capital that were hit by unrest that threatened the ruling coalition\u2019s tight hold on country.\nRights groups accused the government of trying to stop them spreading news online and organising rallies calling for land rights and other freedoms \u2013 charges the government denied.\nAdvertisement\nBut internet users said they had noticed services returning following the April 2 inauguration of Abiy Ahmed.\nThe communications minister and the state-run telecoms monopoly did not immediately reply to requests for comment.\n We are very happy that it is back to normal,  said Hassan Bulcha, who runs an internet cafe in Shashemene, a town in the state of Oromiya which has seen some of the worst violence since protests erupted in 2015.\nGroups that monitor internet usage in Ethiopia \u2013 one of the last countries on the continent with a state telecoms monopoly \u2013 gave the news a guarded welcome.'
print('Size of doc: ' + str(len(example_doc)))
inputs    = tokenizer(example_doc, return_tensors="pt", padding=True).to(device)
translated   = model.generate(**inputs).to(device)
[print(tokenizer.decode(t, skip_special_tokens=True)) for t in translated]
