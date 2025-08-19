/**
 * WebSocket 채팅 클라이언트
 * 실시간 채팅을 위한 WebSocket 연결 및 메시지 처리를 담당
 */
class ChatClient {
    constructor(roomId, username) {
        this.roomId = roomId;
        this.username = username;
        this.socket = null;
        this.isConnected = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 1000; // 1초
        this.heartbeatInterval = null;
        
        // 콜백 함수들
        this.onConnectionStatusChange = window.onConnectionStatusChange || function() {};
        this.onMessageReceived = window.onMessageReceived || function() {};
        this.onError = window.onError || function() {};
    }
    
    /**
     * WebSocket 연결 시작
     */
    connect() {
        try {
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const host = window.location.host;
            const url = `${protocol}//${host}/ws/chat`;
            
            console.log(`WebSocket 연결 시도: ${url}`);
            this.onConnectionStatusChange('connecting');
            
            this.socket = new WebSocket(url);
            this.setupEventHandlers();
            
        } catch (error) {
            console.error('WebSocket 연결 실패:', error);
            this.onConnectionStatusChange('disconnected');
            this.handleReconnect();
        }
    }
    
    /**
     * WebSocket 이벤트 핸들러 설정
     */
    setupEventHandlers() {
        this.socket.onopen = (event) => {
            console.log('WebSocket 연결 성공');
            this.isConnected = true;
            this.reconnectAttempts = 0;
            this.onConnectionStatusChange('connected');
            
            // 채팅방 입장 메시지 전송
            this.sendEnterMessage();
            
            // 하트비트 시작
            this.startHeartbeat();
        };
        
        this.socket.onmessage = (event) => {
            try {
                const chatMessage = JSON.parse(event.data);
                console.log('메시지 수신:', chatMessage);
                this.handleMessage(chatMessage);
            } catch (error) {
                console.error('메시지 파싱 오류:', error);
            }
        };
        
        this.socket.onclose = (event) => {
            console.log('WebSocket 연결 종료:', event.code, event.reason);
            this.isConnected = false;
            this.onConnectionStatusChange('disconnected');
            this.stopHeartbeat();
            
            // 정상 종료가 아닌 경우 재연결 시도
            if (event.code !== 1000 && this.reconnectAttempts < this.maxReconnectAttempts) {
                this.handleReconnect();
            }
        };
        
        this.socket.onerror = (error) => {
            console.error('WebSocket 오류:', error);
            this.onError('WebSocket 연결 오류가 발생했습니다.');
        };
    }
    
    /**
     * 채팅방 입장 메시지 전송
     */
    sendEnterMessage() {
        const enterMessage = {
            type: 'ENTER',
            roomId: this.roomId,
            sender: this.username,
            message: `${this.username}님이 입장하셨습니다.`
        };
        
        this.sendSocketMessage(enterMessage);
    }
    
    /**
     * 일반 채팅 메시지 전송
     */
    sendMessage(message) {
        if (!this.isConnected) {
            console.warn('WebSocket이 연결되지 않았습니다.');
            return false;
        }
        
        const chatMessage = {
            type: 'TALK',
            roomId: this.roomId,
            sender: this.username,
            message: message
        };
        
        return this.sendSocketMessage(chatMessage);
    }
    
    /**
     * 채팅방 퇴장 메시지 전송
     */
    sendQuitMessage() {
        const quitMessage = {
            type: 'QUIT',
            roomId: this.roomId,
            sender: this.username,
            message: `${this.username}님이 퇴장하셨습니다.`
        };
        
        this.sendSocketMessage(quitMessage);
    }
    
    /**
     * WebSocket으로 메시지 전송
     */
    sendSocketMessage(message) {
        try {
            if (this.socket && this.socket.readyState === WebSocket.OPEN) {
                this.socket.send(JSON.stringify(message));
                return true;
            } else {
                console.warn('WebSocket이 열려있지 않습니다. ReadyState:', this.socket?.readyState);
                return false;
            }
        } catch (error) {
            console.error('메시지 전송 실패:', error);
            return false;
        }
    }
    
    /**
     * 수신된 메시지 처리
     */
    handleMessage(chatMessage) {
        // 타임스탬프가 문자열인 경우 Date 객체로 변환
        if (typeof chatMessage.timestamp === 'string') {
            chatMessage.timestamp = new Date(chatMessage.timestamp);
        }
        
        // 메시지 타입별 처리
        switch (chatMessage.type) {
            case 'ENTER':
                this.handleEnterMessage(chatMessage);
                break;
            case 'TALK':
                this.handleTalkMessage(chatMessage);
                break;
            case 'QUIT':
                this.handleQuitMessage(chatMessage);
                break;
            default:
                console.warn('알 수 없는 메시지 타입:', chatMessage.type);
        }
        
        // 콜백 함수 호출
        this.onMessageReceived(chatMessage);
    }
    
    /**
     * 입장 메시지 처리
     */
    handleEnterMessage(chatMessage) {
        console.log(`${chatMessage.sender}님이 입장했습니다.`);
        // 사용자 수 업데이트 (선택사항)
        this.updateUserCount();
    }
    
    /**
     * 일반 채팅 메시지 처리
     */
    handleTalkMessage(chatMessage) {
        // 메시지 내용에서 HTML 태그 제거 (XSS 방지)
        chatMessage.message = this.sanitizeMessage(chatMessage.message);
    }
    
    /**
     * 퇴장 메시지 처리
     */
    handleQuitMessage(chatMessage) {
        console.log(`${chatMessage.sender}님이 퇴장했습니다.`);
        // 사용자 수 업데이트 (선택사항)
        this.updateUserCount();
    }
    
    /**
     * 메시지 내용 정화 (XSS 방지)
     */
    sanitizeMessage(message) {
        const div = document.createElement('div');
        div.textContent = message;
        return div.innerHTML;
    }
    
    /**
     * 접속자 수 업데이트
     */
    updateUserCount() {
        // 서버에서 사용자 수 정보를 받아올 수 있다면 구현
        // 현재는 WebSocket 메시지로 사용자 수를 전달하지 않으므로 생략
    }
    
    /**
     * 하트비트 시작 (연결 유지)
     */
    startHeartbeat() {
        this.heartbeatInterval = setInterval(() => {
            if (this.isConnected && this.socket.readyState === WebSocket.OPEN) {
                // 핑 메시지 전송 (서버에서 핑/퐁을 지원하는 경우)
                try {
                    this.socket.send(JSON.stringify({ type: 'PING' }));
                } catch (error) {
                    console.warn('하트비트 전송 실패:', error);
                }
            }
        }, 30000); // 30초마다
    }
    
    /**
     * 하트비트 중지
     */
    stopHeartbeat() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval);
            this.heartbeatInterval = null;
        }
    }
    
    /**
     * 재연결 처리
     */
    handleReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('최대 재연결 시도 횟수를 초과했습니다.');
            this.onError('서버 연결에 실패했습니다. 페이지를 새로고침해주세요.');
            return;
        }
        
        this.reconnectAttempts++;
        const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1); // 지수 백오프
        
        console.log(`${delay}ms 후 재연결 시도... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
        
        setTimeout(() => {
            this.connect();
        }, delay);
    }
    
    /**
     * WebSocket 연결 종료
     */
    disconnect() {
        console.log('WebSocket 연결을 종료합니다.');
        
        // 퇴장 메시지 전송
        if (this.isConnected) {
            this.sendQuitMessage();
        }
        
        // 재연결 방지
        this.maxReconnectAttempts = 0;
        
        // 하트비트 중지
        this.stopHeartbeat();
        
        // 소켓 닫기
        if (this.socket) {
            this.socket.close(1000, 'User disconnected');
            this.socket = null;
        }
        
        this.isConnected = false;
        this.onConnectionStatusChange('disconnected');
    }
    
    /**
     * 연결 상태 확인
     */
    isConnectionOpen() {
        return this.socket && this.socket.readyState === WebSocket.OPEN;
    }
    
    /**
     * 재연결 시도
     */
    reconnect() {
        this.reconnectAttempts = 0;
        this.connect();
    }
}

// 전역 함수로 ChatClient 제공
window.ChatClient = ChatClient;