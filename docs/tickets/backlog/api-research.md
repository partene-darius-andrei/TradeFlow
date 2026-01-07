# 📝 Documentation - API Research Reference

Effort level: Small
Priority: Low

## Objective

Document key findings from Coinbase API research for future reference.

## Key Constraints

| Constraint | Value | Impact |
| --- | --- | --- |
| REST Rate Limit | 10,000/hour | Use WebSocket for real-time |
| Maker Fee (Intro) | 0.60% | Grid spacing ≥ 1.5% |
| JWT Expiry | 2 minutes | Regenerate per request |
| WS Timeout | 60-90 sec | Must use heartbeats |
| Max Candles | 350/request | Aggregate for H4 |
| Sandbox | Static only | Cannot paper trade |

## Order Type Details

### Bracket Order (`trigger_bracket_gtc`)

- `limit_price` = **Take Profit** (counterintuitive!)
- `stop_trigger_price` = **Stop Loss**
- Do NOT include `base_size` in attached config

### Limit Order (`limit_limit_gtc`)

- Use `post_only: true` for maker fees
- Order rejected if would immediately fill

## Fee Break-Even for Grid Trading

| Tier | Maker Fee | Min Grid Spacing |
| --- | --- | --- |
| Intro | 0.60% | 1.5% |
| Intro 2 | 0.35% | 0.75% |
| Advanced 1 | 0.25% | 0.5% |

## WebSocket Channels

| Channel | URL | Auth | Purpose |
| --- | --- | --- | --- |
| ticker | advanced-trade-ws | No | Price updates |
| heartbeats | advanced-trade-ws | No | Keep alive |
| user | advanced-trade-ws-user | Yes | Order updates |

## JWT Requirements

- Algorithm: **ES256** (ECDSA P-256)
- Must use Coinbase-provided private key
- Include `nonce` in header (random hex)
- URI format: `METHOD host/path` (no https://)

## Sandbox Limitations

- URL: [`api-sandbox.coinbase.com`](http://api-sandbox.coinbase.com)
- Returns static mock data
- Orders never fill
- Market data doesn't update
- **Not usable for strategy testing**

## Testing Alternatives

1. Local simulation with recorded data
2. Small real trades ($10-20)
3. Bitsgap demo mode (third-party)

## Links

- [Coinbase Advanced Trade API](https://docs.cdp.coinbase.com/advanced-trade/docs/welcome)
- [WebSocket Channels](https://docs.cdp.coinbase.com/advanced-trade/docs/ws-channels)
- [Order Types](https://docs.cdp.coinbase.com/advanced-trade/docs/order-types)