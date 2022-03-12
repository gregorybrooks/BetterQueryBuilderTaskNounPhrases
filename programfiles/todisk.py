import json
import copy
import sys
import numpy as np
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity


def write_embedding2(v):
    list_version = np.ndarray.tolist(np.asarray(v))
    json_version = json.dumps(list_version)
    return json_version


def get_score(v):
    s = 0.0
    for example_doc_sentence in v:
        for candidate_sentence_score in example_doc_sentence:
            if candidate_sentence_score > 0.5:
                s += candidate_sentence_score
    return s


#model = SentenceTransformer('sentence-transformers/paraphrase-MiniLM-L6-v2')
model = SentenceTransformer('sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2')

MAX_DOCS = 200000

index1 = {}
doc_sentences = {}

f = open("/mnt/scratch/glbrooks/BETTER_TEST_ENVIRONMENTS/BETTER_MITRE_EVAL_JAN_2021/corpus/arabic/arabic-corpus.jl", "r")
outfile = open("/mnt/scratch/glbrooks/BETTER_TEST_ENVIRONMENTS/BETTER_MITRE_EVAL_JAN_2021/corpus/arabic/embeddings.index", "w")

cnt = 1
for x in f:
  l = json.loads(x)
  id=l['derived-metadata']['id']
  doctext=l['derived-metadata']['text']
  if cnt % 1000 == 0:
      print(cnt)
      sys.stdout.flush()
  segments = l['derived-metadata']['segment-sections']
  sentences = []
  for sentence_descriptor in segments:
      sentences.append(doctext[sentence_descriptor['start']: sentence_descriptor['end']])
  if id == '7f4d422af45406c12d69e978053b37a2':
      doc_sentences[id] = copy.copy(sentences)
  embeddings = model.encode(sentences)
#  index1[id] = copy.copy(embeddings)
  outline = '{"id": "' + id + '", "emb": ' + write_embedding2(embeddings) + '} \n'
  outfile.write(outline)
  cnt += 1
  if cnt > MAX_DOCS:
      break

index2 = {}
with open('embeddings.index') as json_file:
    for line in json_file:
        json_line = json.loads(line)
        id = json_line['id']
        embedding_as_list = json_line['emb']
        x = np.asarray(embedding_as_list)
        index2[id] = x


example_sentences = doc_sentences['7f4d422af45406c12d69e978053b37a2']
example = model.encode(example_sentences)

best = 0.0
best_doc = ''
for key, value in index2.items():
    candidate_similarity_scores = cosine_similarity(example, value)
#    print(key, candidate_similarity_scores)
    candidate_score = get_score(candidate_similarity_scores)
#    print(key, candidate_score)
    if candidate_score > best:
        best = candidate_score
        best_doc = key
print('BEST:', best_doc, best)

#lines = []
#while True:
#    for line in sys.stdin:
#        line = line.strip()
#        if line == 'EOD':
#            example   = model.encode(lines)
#            best = 0.0
#            best_doc = ''
#            for key, value in index2.items():
#                candidate_similarity_scores = cosine_similarity(example, value)
#                print(key, candidate_similarity_scores)
#                candidate_score = get_score(candidate_similarity_scores)
#                print(key, candidate_score)
#                if candidate_score > best:
#                    best = candidate_score
#                    best_doc = key
#            print('BEST:', best_doc, best)
#                        
#
#            sys.stdout.flush()
#            lines.clear()
#        elif line.startswith('EOF'):
#            sys.exit(0)
#        else:
#            lines.append(line)
#

