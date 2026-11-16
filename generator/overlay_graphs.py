#!/usr/bin/env python3
"""
Adds to <sg>/overlays/<platform>/ the SwiftUI-facing symbols that frameworks declare in their own module
(<sg-root>/../sgmain/<platform>/: TipKit, WidgetKit, HealthKitUI…, extracted with swift-symbolgraph-extract) and, under
overlays/xros/, the symbols that only the visionOS SDK has (<sg-root>/../sgxros/). Run before overlays.py.

usage: overlay_graphs.py <scratch-root containing sgnew/, sgmain/, sgxros/>
"""
import json, glob, os, sys
S = sys.argv[1]
VIEWISH = ('View', 'ViewModifier', 'Scene', 'WidgetConfiguration', 'ControlWidgetTemplate', 'Widget', 'WidgetBundle', 'ControlWidget')
def filtered(files):
    out = {}
    for f in files:
        d = json.load(open(f))
        ext = '@SwiftUI' in f
        syms = {s['identifier']['precise']: s for s in d['symbols']}
        views = {r['source'] for r in d.get('relationships', []) if r['kind'] == 'conformsTo'
                 and r.get('targetFallback', '').split('.')[-1] in VIEWISH}
        vnames = {'.'.join(syms[v]['pathComponents']) for v in views if v in syms}
        for u, s in syms.items():
            p = '.'.join(s['pathComponents'])
            if ext or u in views or (any(p.startswith(n + '.') for n in vnames) and s['kind']['identifier'] in ('swift.init', 'swift.method', 'swift.type.method')):
                out[u] = s
    return list(out.values())
def write(plat, module, symbols):
    if not symbols: return
    os.makedirs(f'{S}/sgnew/overlays/{plat}', exist_ok=True)
    json.dump({'module': {'name': module}, 'symbols': symbols, 'relationships': []},
              open(f'{S}/sgnew/overlays/{plat}/{module}@main.symbols.json', 'w'))
for plat in ('ios', 'macos', 'tvos', 'watchos'):
    files = glob.glob(f'{S}/sgmain/{plat}/*.json') + (glob.glob(f'{S}/sgmain/iosdev/*.json') if plat == 'ios' else [])
    mods = {}
    for f in files:
        m = os.path.basename(f).split('@')[0].split('.')[0]
        if m.startswith('_'): m = m.strip('_').replace('_SwiftUI', '').replace('_UIKit', '').replace('_AppKit', '')
        mods.setdefault(m, []).append(f)
    for m, fs in mods.items():
        if m == 'HomeKit': continue  # already in the _HomeKit_SwiftUI overlay
        write(plat, f'_{m}_SwiftUI', filtered(fs))
# visionOS: every symbol missing from the four supported SDKs
known = set()
for f in glob.glob(f'{S}/sgnew/*/*.json') + glob.glob(f'{S}/sgnew/overlays/*/*.json') + glob.glob(f'{S}/sgmain/*/*.json'):
    if '/xros/' in f: continue
    plat = next((p for p in ('ios', 'macos', 'tvos', 'watchos') if '/' + p + '/' in f or '/iosdev/' in f), None)
    dom = {'ios': 'iOS', 'macos': 'macOS', 'tvos': 'tvOS', 'watchos': 'watchOS', None: 'iOS'}[plat]
    for s in json.load(open(f))['symbols']:
        if not any(a.get('isUnconditionallyUnavailable') for a in s.get('availability') or [] if a.get('domain') in ('*', dom)):
            known.add(s['identifier']['precise'])
xr = {}
for f in glob.glob(f'{S}/sgxros/*.json'):
    for s in json.load(open(f))['symbols']:
        if s['identifier']['precise'] not in known: xr[s['identifier']['precise']] = s
write('xros', 'visionOS', list(xr.values()))
print(len(xr), 'visionOS-only symbols')
