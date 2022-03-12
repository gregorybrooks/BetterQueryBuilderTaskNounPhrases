from transformers import MarianTokenizer, MarianMTModel
from typing import List
src = 'en'  # source language
trg = 'ar'  # target language
sample_text = ">>ara<< This is an example of a revolution."
mname = f'data/opus-mt-{src}-{trg}'
example_doc = f'tweet\nEthiopia\u2019s Prime Minister Hailemariam Desalegn said on Thursday he had submitted his resignation as both premier and the chairman of the ruling coalition in an effort to facilitate reforms following a period of mass unrest.\nCAPE TOWNSOUTH AFRICA, 06MAY11 \u2013 Hailemariam Desalegn, Deputy Prime Minister of Ethiopia and Minister of Foreign Affiars of Ethiopia, during the announcement that the 2012 World Economic Forum on Africa will be held in Ethiopia at the World Economic Forum on Africa 2011 held in Cape Town, South Africa, 4-6 May 2011.\nCopyright (cc-by-sa) © World Economic Forum ( www.weforum.org \/Photo Matthew Jordaan [email protected]\nHundreds of people have died in violence sparked initially by an urban development plan for the capital Addis Ababa. The unrest spread in 2015 and 2016 as demonstrations against political restrictions and human rights abuses broke out.\n Unrest and a political crisis have led to the loss of lives and displacement of many,  Hailemariam said in a televised address to the nation.\nAdvertisement\n \u2026 I see my resignation as vital in the bid to carry out reforms that would lead to sustainable peace and democracy,  he said.\nHailemariam said he would stay on as prime minister in a caretaker capacity until the ruling Ethiopian People\u2019s Revolutionary Democratic Front (EPRDF) and the country\u2019s parliament accepted his resignation and named a new premier.\nReporting by Aaron Maasho; Editing by Gareth Jones\nAdvertisement\n'

src_text = [
    '>>fra<< this is a sentence in english that we want to translate to french',
    '>>por<< This should go to portuguese',
    '>>esp<< And this to Spanish'
]

print('Getting tokenizer')
tokenizer = MarianTokenizer.from_pretrained(mname)
#print(tokenizer.supported_language_codes)

print('Getting model')
model = MarianMTModel.from_pretrained(mname)

print('1st translation')
translated = model.generate(**tokenizer(example_doc, return_tensors="pt", padding=True))
print('got tensors')
[print(tokenizer.decode(t, skip_special_tokens=True)) for t in translated]
print('decoded it')
print('2nd translation')
translated = model.generate(**tokenizer(example_doc, return_tensors="pt", padding=True))
print('got tensors')
[print(tokenizer.decode(t, skip_special_tokens=True)) for t in translated]
print('decoded it')
