# 🚀 Milestone: MVP Ready for Testing

Effort level: Small
Priority: High

## Definition of Done

MVP is ready when all the following are complete:

### Core Components

- [ ]  Project setup complete
- [ ]  Domain models defined
- [ ]  SecureKeyStore working
- [ ]  JwtGenerator producing valid tokens
- [ ]  REST API client placing orders
- [ ]  WebSocket client receiving updates
- [ ]  Room database persisting data
- [ ]  Decision engine evaluating correctly
- [ ]  Trading service running 24/7

### Risk Management

- [ ]  Drawdown calculated from high water mark
- [ ]  Emergency liquidation triggers at 15%
- [ ]  All orders cancelled on emergency

### UI (Minimal)

- [ ]  Can enter API credentials
- [ ]  Can start/stop service
- [ ]  Shows current status

### Testing

- [ ]  Unit tests for decision engine pass
- [ ]  Can place and cancel real orders
- [ ]  Service survives 8-hour screen-off test

## Not Required for MVP

- Fancy UI/charts
- Multiple assets
- Advanced analytics
- Cloud backup

## Next Steps After MVP

1. Run with $100 real capital for 1 week
2. Monitor logs and behavior
3. Fix any bugs discovered
4. Gradually increase capital

## Success Criteria

- No crashes in 24-hour test
- Correct mode detection
- Orders placed at correct prices
- Drawdown limit enforced