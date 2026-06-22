import { useEffect, useRef, useState } from 'react';
import { useAuthStore } from '../store/authStore';

export interface WsMessage {
  eventType: 'HEALTH_CHECK' | 'INCIDENT' | 'ALERT';
  data: any;
}

export function useWebsocket(onMessageReceived?: (msg: WsMessage) => void) {
  const { activeOrgId, token } = useAuthStore();
  const [isConnected, setIsConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    if (!activeOrgId || !token) return;

    // Connect to WebSocket service routed through gateway
    const wsUrl = `ws://localhost:8080/ws/dashboard?orgId=${activeOrgId}`;
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      setIsConnected(true);
      console.log('WebSocket connected to PulseOps dashboard stream');
    };

    ws.onmessage = (event) => {
      try {
        const msg: WsMessage = JSON.parse(event.data);
        if (onMessageReceived) {
          onMessageReceived(msg);
        }
      } catch (e) {
        console.error('Failed to parse WebSocket event data', e);
      }
    };

    ws.onclose = () => {
      setIsConnected(false);
      console.log('WebSocket connection closed');
    };

    ws.onerror = (err) => {
      console.error('WebSocket stream error', err);
    };

    return () => {
      ws.close();
      wsRef.current = null;
    };
  }, [activeOrgId, token, onMessageReceived]);

  return { isConnected };
}
