# QR-Based Restaurant Ordering & Kitchen Management System

A comprehensive backend system for restaurant ordering via QR codes with real-time kitchen management, payment integration, and table booking capabilities.

## 🎯 Features

### Core Functionality
- ✅ **QR Code Ordering**: Scan table QR codes to start ordering
- ✅ **Digital Menu**: Browse categorized menu with filters (veg/non-veg, allergens, spice levels)
- ✅ **Order Management**: Place, modify, and cancel orders
- ✅ **Real-time Kitchen Display**: WebSocket-based live order updates
- ✅ **Payment Integration**: Razorpay/UPI with refund support
- ✅ **Table Booking**: Reserve tables with date/time slots
- ✅ **Admin Dashboard**: Manage menu, tables, staff, and view analytics
- 🔒 **Security**: JWT authentication, QR token validation, HMAC signatures
- 🔄 **Concurrency Control**: Optimistic locking with MongoDB versioning
- ⚡  **Real-time Updates**: WebSocket for kitchen and customer notifications
- 📊 **Analytics**: Daily reports, popular items tracking, revenue metrics
- 🔔 **Notifications**: Booking reminders, order status updates
- 🗓️ **Scheduled Tasks**: Auto-close inactive sessions, payment timeout handling
- 🐳 **Containerization**: Docker and Kubernetes deployment ready
- ✅ **Validation**: Comprehensive input validation and error handling

## 🏗️ Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   Customer  │────▶│   REST API   │────▶│   MongoDB   │
│  (QR Scan)  │     │  Spring Boot │     │  Database   │
└─────────────┘     └──────────────┘     └─────────────┘
                           │
                           ├──────────▶ Razorpay Gateway
                           │
                    ┌──────▼──────┐
                    │  WebSocket  │
                    │   Server    │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │   Kitchen   │
                    │   Display   │
                    └─────────────┘
```

## 📦 Tech Stack

- **Backend**: Spring Boot 3.x
- **Database**: MongoDB (Document-based)
- **Real-time**: WebSocket (STOMP)
- **Payment**: Razorpay SDK
- **Security**: Spring Security + JWT
- **Deployment**: Docker, Kubernetes
- **Testing**: JUnit 5, Mockito

## 🚀 Getting Started

### Prerequisites
- Java 17+
- MongoDB 6.0+
- Maven 3.8+
- Razorpay Account (for payments)

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/raushan736834/BlackMagic.git
cd BlackMagic
```

2. **Configure environment variables**
```bash
cp .env.example .env
# Edit .env with your credentials
```

3. **Run with Docker Compose**
```bash
docker-compose up -d
```

4. **Or run locally**
```bash
mvn clean install
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

## 📚 API Documentation

### Customer APIs

#### Start Session
```http
POST /api/customer/session/start
Content-Type: application/json

{
  "qrToken": "unique-qr-token",
  "partySize": 4,
  "deviceId": "device-123"
}
```

#### Get Menu
```http
GET /api/customer/menu
```

#### Create Order
```http
POST /api/customer/orders
Content-Type: application/json

{
  "sessionCode": "SES-ABC123",
  "items": [
    {
      "menuItemId": "item-id",
      "quantity": 2,
      "specialRequest": "No onions"
    }
  ],
  "specialInstructions": "Extra spicy"
}
```

#### Cancel Order
```http
DELETE /api/customer/orders/{orderId}
Content-Type: application/json

{
  "reason": "Changed mind"
}
```

#### Initiate Payment
```http
POST /api/customer/payments/initiate
Content-Type: application/json

{
  "orderId": "order-id",
  "method": "UPI"
}
```

### Kitchen APIs

#### Get Active Orders
```http
GET /api/kitchen/orders
Authorization: Bearer {jwt-token}
```

#### Update Order Status
```http
PATCH /api/kitchen/orders/{orderId}/status
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
  "status": "READY",
  "staffId": "staff-id"
}
```

### Admin APIs

#### Login
```http
POST /api/admin/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}
```

#### Create Table
```http
POST /api/admin/tables
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
  "tableNumber": 5,
  "capacity": 4,
  "location": "Indoor"
}
```

#### Daily Analytics
```http
GET /api/admin/analytics/daily/2024-01-15
Authorization: Bearer {jwt-token}
```

## 🔌 WebSocket Integration

### Kitchen Display Connection
```javascript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const socket = new SockJS('http://localhost:8080/ws/kitchen');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  // Subscribe to new orders
  stompClient.subscribe('/topic/kitchen/orders', (message) => {
    const order = JSON.parse(message.body);
    console.log('New order:', order);
  });
  
  // Subscribe to updates
  stompClient.subscribe('/topic/kitchen/updates', (message) => {
    const update = JSON.parse(message.body);
    console.log('Order update:', update);
  });
});

// Update order status
function markOrderReady(orderId) {
  stompClient.send(
    `/app/kitchen/order/${orderId}/status`,
    {},
    JSON.stringify({ status: 'READY' })
  );
}
```

### Customer Table Connection
```javascript
const sessionCode = 'SES-ABC123';
const socket = new SockJS('http://localhost:8080/ws/table');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  // Subscribe to table updates
  stompClient.subscribe(`/topic/table/${sessionCode}/updates`, (message) => {
    const update = JSON.parse(message.body);
    console.log('Order status:', update);
  });
});
```

## 🗄️ Database Schema

### Collections

1. **tables** - Restaurant tables with QR codes
2. **table_sessions** - Active customer sessions
3. **menu_categories** - Food categories
4. **menu_items** - Menu items with details
5. **orders** - Customer orders
6. **payments** - Payment transactions
7. **table_bookings** - Table reservations
8. **admin_users** - Staff and admin users

### Key Indexes
```javascript
// Optimized indexes for performance
db.tables.createIndex({ qrToken: 1 }, { unique: true });
db.orders.createIndex({ sessionId: 1, status: 1, createdAt: -1 });
db.payments.createIndex({ orderId: 1, transactionId: 1 });
db.table_sessions.createIndex({ sessionCode: 1, status: 1 });
```

## 🔐 Security Features

### QR Token Validation
- HMAC-signed tokens
- Token rotation capability
- Server-side validation

### Payment Security
- Razorpay signature verification
- Webhook signature validation
- Idempotency keys for retry safety

### API Security
- JWT-based authentication
- Role-based access control (RBAC)
- Rate limiting on payment endpoints
- CORS configuration

## 📊 Order Lifecycle

```
PLACED → IN_KITCHEN → PREPARING → READY → SERVED
   ↓
CANCELLED (with refund if paid)
```

### Cancellation Rules
- 5-minute window for customer cancellations
- Automatic refund initiation
- Kitchen can reject orders if items unavailable

## 🔄 Background Tasks

### Scheduled Jobs

1. **Session Cleanup** (Every 15 min)
   - Closes inactive sessions after 30 minutes

2. **Payment Timeout** (Every 5 min)
   - Expires unpaid orders after 15 minutes

3. **Booking Reminders** (Hourly)
   - Sends reminders 2 hours before booking

4. **Daily Analytics** (2 AM daily)
   - Generates revenue and performance reports

5. **Popular Items Update** (2:30 AM daily)
   - Updates "popular" tags based on last 7 days

## 🧪 Testing

### Run Tests
```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=OrderServiceTest

# Integration tests only
mvn test -Dtest=*IntegrationTest
```

### Test Coverage
- Unit tests for all services
- Integration tests for API endpoints
- Mock external dependencies (Razorpay)

## 🚢 Deployment

### Docker Deployment
```bash
# Build and run
docker-compose up -d

# View logs
docker-compose logs -f blackmagic

# Stop services
docker-compose down
```

### Kubernetes Deployment
```bash
# Apply configurations
kubectl apply -f k8s/

# Check status
kubectl get pods
kubectl get services

# Scale replicas
kubectl scale deployment blackmagic --replicas=5
```

### Environment Variables
```bash
RAZORPAY_KEY_ID=rzp_test_xxxxx
RAZORPAY_KEY_SECRET=secret_key
RAZORPAY_WEBHOOK_SECRET=webhook_secret
JWT_SECRET=your_jwt_secret_min_256_bits
MONGODB_URI=mongodb://localhost:27017/db
```

## 📈 Monitoring

### Health Check
```http
GET /actuator/health
```

### Metrics
```http
GET /actuator/metrics
GET /actuator/metrics/http.server.requests
GET /actuator/metrics/jvm.memory.used
```

### Logs
- Structured logging with SLF4J
- Request/response logging
- Error tracking with stack traces

## 🔧 Configuration

### application.properties
```properties
# Server
server.port=8080

# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/db

# Session
session.inactive.timeout.minutes=30
session.auto.close.hours=4

# Order
order.cancellation.window.minutes=5
order.payment.timeout.minutes=15

# JWT
jwt.expiration.ms=86400000
```

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 👥 Support

For issues and questions:
- Email: raushan736834@gmail.com
- GitHub Issues: [Create Issue](https://github.com/raushan736834/BlackMagic/issues)

## 🗺️ Roadmap

### Phase 1 (Completed) ✅
- QR scanning and ordering
- Payment integration
- Kitchen display
- Basic admin panel

### Phase 2 (In Progress) 🚧
- AI-based recommendations
- Multi-language menu support
- Advanced analytics dashboard
- Inventory management

### Phase 3 (Planned) 📋
- Loyalty program integration
- Customer feedback system
- Waitlist management
- Third-party delivery integration

---

**Built with ❤️ for modern restaurants**