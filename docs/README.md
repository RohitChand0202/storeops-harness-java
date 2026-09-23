# docs/

Deployment and run evidence. Commit the images — a screenshot referenced in `DEPLOYMENT.md` but not
committed reads as a missing artefact.

Expected contents after the demonstration run:

| File | Shows |
|---|---|
| `deployment-container.png` | The running container (`docker compose ps`, or the cloud console) |
| `deployment-bulk-status-207.png` | `PATCH /api/activities/bulk-status` returning 207 with populated `succeeded[]` and `failed[]` |
| `DESIGN_BRIEF.pdf` | Rendered brief — included to evidence the 3–5 page requirement |

Capture the 207 partial-failure case rather than the 200 all-success case. A 200 shows the endpoint
exists; a 207 shows the partial-failure semantics the sprint contract specified actually work in a
running application, which is what the harness was built to govern.
