# knowledge-service CSV import templates

Header-only templates for loading data extracted from official sources (rice pilot Phase 1,
`docs/plans/rice-knowledge-pilot.md`). Fill them in a spreadsheet, save as **CSV UTF-8** (keeps
Bangla text), then import:

```bash
curl -X POST --data-binary @sources.csv -H "Content-Type: text/csv" http://localhost:8080/api/v1/knowledge/import/sources
curl -X POST --data-binary @pesticides.csv -H "Content-Type: text/csv" http://localhost:8080/api/v1/knowledge/import/pesticides
```

| File | Import path | Notes |
|---|---|---|
| `sources.csv` | `sources` | **Import first.** One row per document (title, publisher, edition, year, URL, language, licence note, `retrievedAt` as YYYY-MM-DD) |
| `pesticides.csv` | `pesticides` | One row per registered use (product × crop × target). `registrationStatus`: REGISTERED, DEREGISTERED, BANNED, UNKNOWN |
| `fertilizer-recommendations.csv` | `fertilizer-recommendations` | Doses in **kg per hectare** as in the source. `season`: AUS, AMAN, BORO, RABI, KHARIF, YEAR_ROUND |
| `crop-calendar.csv` | `crop-calendar` | Window as month/day numbers; may wrap the year end (start 11, end 1). Empty `region` = country-wide. `activity`: SEEDBED, SOWING, TRANSPLANTING, FERTILIZING, WEEDING, IRRIGATION, HARVEST, OTHER |
| `pests-diseases.csv` | `pests-diseases` | `kind`: PEST, DISEASE, DEFICIENCY, OTHER. Chemical control = active ingredients only; products/doses come from `pesticides.csv` |

Rules:
- **Every fact row needs a source**: fill `sourceTitle` (exact title from `sources.csv`) or `sourceId`.
  Use `sourceId` if two editions share a title. Fill `sourcePage` so answers can cite the page.
- `crop` is case-insensitive and stored upper case (`rice` → `RICE`).
- Copy values exactly as the source states them; don't convert units or round. Unknown → leave blank.
- Rows import independently. The response lists failed rows by spreadsheet row number with the
  reason (bad number, unknown enum, duplicate, missing source); fix those rows and re-import just them.
