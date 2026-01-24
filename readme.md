# Lucrări Studenți – Proiect Git

## Descriere

Acest repository conține lucrările studenților, organizate pe branch-uri separate. Fiecare student are propriul branch, de obicei cu numele utilizatorului (ex: `josu_ion`, `cocieru_emil`). Branch-ul principal, `main`, conține versiunea consolidată și/sau instrucțiuni generale.[web:17][web:18]

Pentru a vizualiza lucrările unui anumit student, trebuie să comuți pe branch-ul corespunzător folosind Git sau interfața web GitHub.[web:16][web:19]

---

## Cum comuți între branch-uri (linie de comandă)

1. **Comută pe branch-ul studentului dorit**

   ```bash
   git checkout [nume-student]

   git checkout josu_ion      # Lucrările lui Josu Ion
   git checkout cocieru_emil  # Lucrările lui Cocieru Emil

Pentru a reveni in branchiul principal apelam
   ```bash
   git checkout main
