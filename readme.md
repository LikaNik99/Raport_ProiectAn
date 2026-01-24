README.md - Lucrări Studenți Proiect Git
Descriere
Acest repository conține lucrările studenților organizați pe branch-uri separate. Fiecare student are propriul său branch cu numele utilizatorului (ex: student1, student2). Branch-ul principal main conține versiunea consolidată sau instrucțiunile generale.

Pentru a vizualiza lucrările unui anumit student, trebuie să comuți branch-ul corespunzător.

Instrucțiuni rapide pentru comutare branch
1. Clonează repository-ul (prima dată)
bash
git clone https://github.com/[utilizator]/[nume-repo].git
cd [nume-repo]
2. Vezi toate branch-urile disponibile
bash
git branch -a
Sau online pe GitHub: Code → Branches

3. Comută pe branch-ul studentului dorit
bash
git checkout [nume-student]
Exemple:

bash
git checkout student1     # Lucrările lui Student1
git checkout ana-popescu  # Lucrările Anei Popescu
git checkout ion-marin    # Lucrările lui Ion Marin
4. Întoarce-te pe main
bash
git checkout main
Comenzi utile
Acțiune	Comandă
Lista branch-urilor locale	git branch
Lista tuturor branch-urilor	git branch -a
Actualizează lista remote branch-urilor	git fetch
Șterge branch local	git branch -d [nume]
Creează branch nou	git checkout -b [nume- nou]
Structura branch-urilor (exemplu)
text
main/              ← Instrucțiuni generale
├── README.md
└── setup/
student1/          ← Lucrări Student1
├── src/
├── tests/
└── docs/
student2/          ← Lucrări Student2
└── ...
Vizualizare online (fără Git)
Intră pe GitHub repository

Click "branch: main" (dropdown în stânga sus)

Selectează branch-ul studentului dorit

Navighează prin fișiere

Probleme comune & soluții
❌ "Branch-ul nu există"

bash
git fetch origin
git checkout [nume-student]
❌ "Fișierele nu se văd"

Asigură-te că ești pe branch-ul corect: git branch (branch-ul activ e marcat cu *)

Refresh GitHub sau re-clonează repo-ul

Contribuții
Fiecare student lucrează pe propriul branch

Pentru merge în main: creează Pull Request

Respectă naming convention: nume-prenume sau username