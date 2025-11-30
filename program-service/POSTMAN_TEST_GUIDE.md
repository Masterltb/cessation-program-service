# Hướng dẫn Kiểm thử API bằng Postman

Tài liệu này cung cấp một kịch bản kiểm thử (test flow) chi tiết, theo đúng thứ tự, mô phỏng một luồng sử dụng hoàn chỉnh cho `program-service`.

---

### **Phần A: Chuẩn bị Môi trường**

Trước khi bắt đầu, hãy đảm bảo bạn đã thiết lập các biến môi trường (Environment Variables) trong Postman.

1.  Mở Postman, chọn tab **Environments** ở thanh bên trái.
2.  Tạo một môi trường mới (ví dụ: `Program Service - Môi trường Dev`).
3.  Thêm các biến sau và điền giá trị tương ứng:

| Tên Biến  | Giá trị Mẫu                               | Mô tả                                            |
| :-------- | :---------------------------------------- | :----------------------------------------------- |
| `baseUrl` | `http://localhost:8080`                   | URL cơ sở của môi trường API đang chạy.           |
| `userId`  | `a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11`    | UUID của một người dùng test với vai trò `CUSTOMER`. |
| `adminId` | `a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12`    | UUID của một người dùng test với vai trò `ADMIN`.    |
| `coachId` | `a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13`    | UUID của một người dùng test với vai trò `COACH`.    |

---

### **Phần B: Luồng Người dùng (Vai trò: CUSTOMER)**

Kịch bản này mô phỏng một người dùng mới đăng ký, thực hiện chương trình và tương tác với các tính năng.

**Bước 1: Gửi Đánh giá Ban đầu**
-   **Mục đích:** Người dùng mới trả lời bộ câu hỏi để hệ thống đánh giá và gợi ý lộ trình.
-   **Hành động:** Mở thư mục `1. User-Facing APIs` -> `Onboarding`, chọn request `POST Submit Baseline Assessment` và nhấn **Send**.
-   **Request:** `POST {{baseUrl}}/api/onboarding/baseline`
-   **Headers:**
    ```
    X-User-Id: {{userId}}
    X-User-Role: CUSTOMER
    Content-Type: application/json
    ```
-   **Body:**
    ```json
    {
      "answers": [
        { "questionId": "q1", "score": 4 },
        { "questionId": "q2", "score": 5 },
        { "questionId": "q3", "score": 3 }
      ]
    }
    ```
-   **Kết quả mong đợi:**
    -   Status `200 OK`.
    -   Script test sẽ tự động lưu `recommendedTemplateId` vào biến để sử dụng sau này.

**Bước 2: Tạo Chương trình Mới**
-   **Mục đích:** Người dùng chọn và tạo một chương trình trả phí dựa trên số ngày.
-   **Hành động:** Mở thư mục `1. User-Facing APIs` -> `Programs`, chọn request `POST Create Program` và nhấn **Send**.
-   **Request:** `POST {{baseUrl}}/v1/programs`
-   **Headers:**
    ```
    X-User-Id: {{userId}}
    X-User-Role: CUSTOMER
    Content-Type: application/json
    ```
-   **Body:**
    ```json
    {
        "planDays": 30
    }
    ```
-   **Kết quả mong đợi:**
    -   Status `201 Created`.
    -   Script test sẽ tự động lưu `id` của chương trình mới vào biến `programId`.

**Bước 3: Xác nhận Chương trình đang hoạt động**
-   **Mục đích:** Kiểm tra xem chương trình vừa tạo có phải là chương trình đang hoạt động của người dùng hay không.
-   **Hành động:** Mở thư mục `1. User-Facing APIs` -> `Programs`, chọn request `GET Active Program` và nhấn **Send**.
-   **Request:** `GET {{baseUrl}}/v1/programs/active`
-   **Headers:**
    ```
    X-User-Id: {{userId}}
    X-User-Role: CUSTOMER
    ```
-   **Body:** (Không có)
-   **Kết quả mong đợi:**
    -   Status `200 OK`.
    -   Body trả về chứa thông tin của chương trình có ID là `{{programId}}`.

**Bước 4: Xem nhiệm vụ hôm nay**
-   **Mục đích:** Người dùng xem các nhiệm vụ cần làm trong ngày.
-   **Hành động:** Mở thư mục `1. User-Facing APIs` -> `Program Execution`, chọn request `GET Today's Steps` và nhấn **Send**.
-   **Request:** `GET {{baseUrl}}/api/programs/{{programId}}/steps/today`
-   **Headers:**
    ```
    X-User-Id: {{userId}}
    X-User-Role: CUSTOMER
    ```
-   **Body:** (Không có)
-   **Kết quả mong đợi:**
    -   Status `200 OK`.
    -   Script test sẽ tự động lưu `id` của nhiệm vụ đầu tiên vào biến `stepId`.

**Bước 5: Hoàn thành một nhiệm vụ**
-   **Mục đích:** Người dùng đánh dấu một nhiệm vụ là đã hoàn thành.
-   **Hành động:** Mở thư mục `1. User-Facing APIs` -> `Program Execution`, chọn request `PATCH Update Step Status` và nhấn **Send**.
-   **Request:** `PATCH {{baseUrl}}/api/programs/{{programId}}/steps/{{stepId}}/status`
-   **Headers:**
    ```
    X-User-Id: {{userId}}
    X-User-Role: CUSTOMER
    Content-Type: application/json
    ```
-   **Body:**
    ```json
    {
        "status": "COMPLETED"
    }
    ```
-   **Kết quả mong đợi:**
    -   Status `200 OK`.
    -   Body trả về cho thấy `status` của step đã được cập nhật.

**Bước 6: Ghi lại một sự kiện hút thuốc (Slip)**
-   **Mục đích:** Người dùng ghi lại một lần lỡ hút thuốc, điều này sẽ ảnh hưởng đến chuỗi ngày không hút.
-   **Hành động:** Mở thư mục `1. User-Facing APIs` -> `Program Execution`, chọn request `POST Create Smoke Event` và nhấn **Send**.
-   **Request:** `POST {{baseUrl}}/api/programs/{{programId}}/smoke-events`
-   **Headers:**
    ```
    X-User-Id: {{userId}}
    X-User-Role: CUSTOMER
    Content-Type: application/json
    ```
-   **Body:**
    ```json
    {
        "occurredAt": "{{$isoTimestamp}}",
        "kind": "SLIP",
        "trigger": "STRESS",
        "note": "Căng thẳng vì công việc."
    }
    ```
-   **Kết quả mong đợi:**
    -   Status `201 Created`.

---

### **Phần C: Luồng Quản trị viên (Vai trò: ADMIN)**

Kịch bản này mô phỏng một quản trị viên tạo nội dung mới cho hệ thống.

**Bước 7: Tạo một Quiz mới**
-   **Mục đích:** Admin tạo một bộ câu hỏi quiz mới cho hệ thống.
-   **Hành động:** Mở thư mục `2. Admin-Facing APIs` -> `Content & Template Management`, chọn request `POST Create Quiz` và nhấn **Send**.
-   **Request:** `POST {{baseUrl}}/v1/admin/quizzes`
-   **Headers:**
    ```
    X-User-Id: {{adminId}}
    X-User-Role: ADMIN
    Content-Type: application/json
    ```
-   **Body:**
    ```json
    {
        "name": "Quiz kiểm tra hàng tuần {{$randomInt}}",
        "languageCode": "vi",
        "questions": [
            {
                "questionNo": 1,
                "text": "Trong tuần qua, bạn có cảm thấy căng thẳng không?",
                "type": "SINGLE_CHOICE",
                "choices": [
                    { "labelCode": "A", "labelText": "Không hề" },
                    { "labelCode": "B", "labelText": "Một chút" },
                    { "labelCode": "C", "labelText": "Rất nhiều" }
                ]
            }
        ]
    }
    ```
-   **Kết quả mong đợi:**
    -   Status `201 Created`.
    -   Script test sẽ tự động lưu `id` của quiz mới vào biến `quizTemplateId`.

**Bước 8: Xuất bản Quiz**
-   **Mục đích:** Admin xuất bản quiz từ trạng thái `DRAFT` để người dùng có thể bắt đầu làm.
-   **Hành động:** Mở thư mục `2. Admin-Facing APIs` -> `Content & Template Management`, chọn request `POST Publish Quiz` và nhấn **Send**.
-   **Request:** `POST {{baseUrl}}/v1/admin/quizzes/{{quizTemplateId}}/publish`
-   **Headers:**
    ```
    X-User-Id: {{adminId}}
    X-User-Role: ADMIN
    ```
-   **Body:** (Không có)
-   **Kết quả mong đợi:**
    -   Status `200 OK`.

---

Bằng cách thực hiện tuần tự 8 bước trên, bạn đã kiểm tra được một luồng nghiệp vụ hoàn chỉnh, bao gồm cả tương tác của người dùng và quản trị viên, đồng thời xác minh được tính liên kết giữa các API.
```