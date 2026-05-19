# Grafana Dashboard Capture Process

This process captures a long Grafana dashboard from the Codex in-app browser and stitches the captured sections into one PNG for phase evidence.

## Scope

- Target Grafana host: `http://localhost:3000`
- Default local login:
  - username: `admin`
  - password: `admin`
- Target dashboard:
  - `/d/db-lab-overview/db-lab-overview`
- Default dashboard URL:

```text
http://localhost:3000/d/db-lab-overview/db-lab-overview?orgId=1&from=now-30m&to=now&timezone=browser&var-phase=phase-02&var-scenario=products&var-preset=baseline&var-pool=pool10&var-uri=$__all&var-table=$__all&refresh=10s
```

Capture phase evidence after the k6 load test has completed and the dashboard values have settled.

## Dashboard Variables

Build the dashboard URL from these query parameters:

- `var-phase`, for example `phase-02`
- `var-scenario`, for example `products`
- `var-preset`, for example `baseline`
- `var-pool`, for example `pool10`
- `var-uri`, for example `$__all`
- `var-table`, for example `$__all`

Use `var-preset`. If a request says `var-oreset`, treat it as a typo unless the dashboard actually defines that variable.

## Browser Flow

1. Connect to the Codex in-app browser through the Browser plugin.
2. Show the in-app browser when the user wants to watch or prepare the dashboard.
3. Open `http://localhost:3000`.
4. If Grafana shows the login page, enter:
   - username: `admin`
   - password: `admin`
5. Submit the login form.
6. If Grafana asks to change the default password, skip the change when a skip option is available.
7. Navigate to the dashboard URL with the requested variables.
8. Wait until the dashboard renders.
9. Ask the user to prepare the dashboard state:
   - expand sections that should appear in the final image
   - collapse sections that should stay collapsed
   - adjust variables or time range if needed
10. Do not start the screenshot sequence until the user says the dashboard is ready.

## Capture Flow

Grafana dashboards often do not scroll at the document level. The visible page can report `document.scrollingElement.scrollHeight == clientHeight` while the real dashboard content scrolls inside an internal `overflow-y: auto` container.

Use this detection rule before capture:

1. Check document-level scroll metrics.
2. Find elements where:
   - `scrollHeight > clientHeight + 20`
   - `overflowY` is `auto`, `scroll`, or `overlay`
3. Choose the largest candidate by `scrollHeight - clientHeight`.
4. Treat that element as the dashboard scroll container.

Then:

1. Scroll the dashboard container to the top.
2. Capture the viewport.
3. Scroll the dashboard container down by slightly less than its visible height.
4. Capture each viewport.
5. Repeat until `scrollTop + clientHeight >= scrollHeight`.
6. Save captures using filenames that include the scroll offset:

```text
part-01-scroll0.png
part-02-scroll614.png
part-03-scroll1228.png
part-04-scroll1676.png
```

The scroll offset in the filename is used by the stitching script.

## Stitching Flow

Use the project script:

```powershell
rtk python scripts/stitch_grafana_dashboard.py `
  --input-dir grafana-internal-scroll-captures `
  --output grafana-dashboard-stitched-full.png `
  --header-height 152 `
  --body-x 0 `
  --body-y 152 `
  --body-width 1060 `
  --body-height 694
```

The script:

1. Reads `*.png` files from the input directory.
2. Parses `scroll<N>` from each filename.
3. Sorts captures by scroll offset.
4. Copies the top header from the first screenshot.
5. Crops the dashboard body area from each screenshot.
6. Pastes each crop at `header-height + scroll offset`.
7. Writes one stitched PNG.

If the viewport size changes, update `--header-height`, `--body-x`, `--body-y`, `--body-width`, and `--body-height` from the detected dashboard container rectangle.

## Final Image Verification

After the stitch script completes, the operator must open the final PNG and visually verify:

- no panels are clipped
- no dashboard sections are missing
- no duplicated scroll regions are visible
- the header appears only once
- panel titles, legends, axes, and key values are readable
- no unexpected `Loading`, query error, or unintended `No data` panel is visible

## Completion Checklist

- Grafana is logged in.
- Dashboard URL includes the requested variables.
- User has confirmed the expand/collapse state is ready.
- k6 load test has completed and dashboard values have settled.
- Internal dashboard scroll container was detected.
- Section captures were saved with `scroll<N>` filenames.
- Stitch script completed successfully.
- Operator opened the final PNG and completed visual verification.
