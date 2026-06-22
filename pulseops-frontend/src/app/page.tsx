"use client";

import React, { useState, useEffect } from 'react';
import { 
  Activity, Shield, Bell, CheckCircle, AlertTriangle, Play, Pause, 
  Trash2, Plus, LogOut, ArrowRight, Server, Globe, Key, User,
  Building, RefreshCw, Send, Zap, Check, HelpCircle
} from 'lucide-react';
import { 
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend
} from 'recharts';
import { useAuthStore } from '../store/authStore';
import { apiFetch } from '../lib/api';
import { useWebsocket } from '../hooks/useWebsocket';

interface Monitor {
  id: number;
  name: string;
  type: string;
  url: string;
  method: string;
  headers?: string;
  requestBody?: string;
  expectedStatusCode: number;
  expectedResponseTime: number;
  checkInterval: number;
  status: string;
  healthStatus: string;
}

interface Incident {
  id: number;
  title: string;
  description: string;
  severity: string;
  status: string;
  startedAt: string;
  resolvedAt?: string;
}

interface LatencyDataPoint {
  time: string;
  [key: string]: string | number; // Dynamic monitor latency values
}

export default function Home() {
  // Global Session State
  const { isAuthenticated, user, organizations, activeOrgId, setSession, clearSession, setActiveOrgId } = useAuthStore();

  // Auth UI state
  const [isLoginView, setIsLoginView] = useState(true);
  const [authEmail, setAuthEmail] = useState('');
  const [authPassword, setAuthPassword] = useState('');
  const [authFirstName, setAuthFirstName] = useState('');
  const [authLastName, setAuthLastName] = useState('');
  const [authOrgName, setAuthOrgName] = useState('');
  const [authError, setAuthError] = useState<string | null>(null);
  const [authSuccess, setAuthSuccess] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // App UI state
  const [activeTab, setActiveTab] = useState<'dashboard' | 'monitors' | 'incidents'>('dashboard');
  const [monitors, setMonitors] = useState<Monitor[]>([]);
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [wsLogs, setWsLogs] = useState<any[]>([]);
  const [latencyData, setLatencyData] = useState<LatencyDataPoint[]>([]);

  // Create Monitor form state
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newMonitorName, setNewMonitorName] = useState('');
  const [newMonitorUrl, setNewMonitorUrl] = useState('');
  const [newMonitorMethod, setNewMonitorMethod] = useState('GET');
  const [newMonitorInterval, setNewMonitorInterval] = useState(30);
  const [newMonitorExpectedCode, setNewMonitorExpectedCode] = useState(200);
  const [newMonitorExpectedTime, setNewMonitorExpectedTime] = useState(1000);
  const [newMonitorHeaders, setNewMonitorHeaders] = useState('');
  const [monitorError, setMonitorError] = useState<string | null>(null);

  // Real-time WebSocket integration
  const { isConnected } = useWebsocket((msg) => {
    // 1. Add to activity feed logs
    setWsLogs((prev) => [
      { id: Date.now(), timestamp: new Date().toLocaleTimeString(), ...msg },
      ...prev.slice(0, 19)
    ]);

    // 2. Handle Monitor Health Check updates
    if (msg.eventType === 'HEALTH_CHECK') {
      const checkData = msg.data;
      
      // Update monitors list state
      setMonitors((prev) => 
        prev.map((mon) => 
          mon.id === checkData.monitorId 
            ? { ...mon, healthStatus: checkData.status }
            : mon
        )
      );

      // Append data point to latency chart
      setLatencyData((prev) => {
        const timeStr = new Date(checkData.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
        const lastPoints = prev.slice(-14); // Keep last 15 points
        
        // Find if we already have a point for this exact second, or create new
        const existingIndex = lastPoints.findIndex(p => p.time === timeStr);
        if (existingIndex !== -1) {
          lastPoints[existingIndex] = {
            ...lastPoints[existingIndex],
            [checkData.name]: checkData.responseTimeMs
          };
          return [...lastPoints];
        } else {
          return [
            ...lastPoints,
            {
              time: timeStr,
              [checkData.name]: checkData.responseTimeMs
            }
          ];
        }
      });
    }

    // 3. Handle Incident updates
    if (msg.eventType === 'INCIDENT') {
      const incData = msg.data;
      setIncidents((prev) => {
        const exists = prev.some(i => i.id === incData.incidentId);
        if (exists) {
          return prev.map(i => i.id === incData.incidentId ? {
            ...i,
            status: incData.status,
            resolvedAt: incData.resolvedAt
          } : i);
        } else {
          return [
            {
              id: incData.incidentId,
              title: incData.title,
              description: incData.description || 'No description provided',
              severity: incData.severity,
              status: incData.status,
              startedAt: incData.startedAt
            },
            ...prev
          ];
        }
      });
    }
  });

  // Load Initial App Data on authentication
  useEffect(() => {
    if (isAuthenticated && activeOrgId) {
      loadMonitors();
      loadIncidents();
      // Initialize mock latency structure if empty
      setLatencyData([
        { time: '00:00', 'API Gateway': 120, 'Payment Endpoint': 340 },
        { time: '01:00', 'API Gateway': 140, 'Payment Endpoint': 280 },
        { time: '02:00', 'API Gateway': 130, 'Payment Endpoint': 410 },
      ]);
    }
  }, [isAuthenticated, activeOrgId]);

  const loadMonitors = async () => {
    try {
      const data = await apiFetch<Monitor[]>('/api/v1/monitors');
      setMonitors(data);
    } catch (e) {
      console.error('Failed to load monitors', e);
    }
  };

  const loadIncidents = async () => {
    try {
      const data = await apiFetch<Incident[]>('/api/v1/incidents');
      setIncidents(data);
    } catch (e) {
      console.error('Failed to load incidents', e);
    }
  };

  // Auth Operations
  const handleAuthSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setAuthError(null);
    setAuthSuccess(null);
    setLoading(true);

    try {
      if (isLoginView) {
        // Login call
        const response = await apiFetch<any>('/api/v1/auth/login', {
          method: 'POST',
          bodyData: { email: authEmail, password: authPassword }
        });
        
        setSession(
          response.accessToken,
          response.refreshToken,
          { id: response.userId, email: response.email, firstName: '', lastName: '', roles: response.roles },
          response.organizations
        );
      } else {
        // Sign up call
        await apiFetch<any>('/api/v1/auth/signup', {
          method: 'POST',
          bodyData: {
            email: authEmail,
            password: authPassword,
            firstName: authFirstName,
            lastName: authLastName,
            organizationName: authOrgName
          }
        });
        setAuthSuccess("Signup successful! You can now log in.");
        setIsLoginView(true);
      }
    } catch (err: any) {
      setAuthError(err.message || "Authentication failed");
    } finally {
      setLoading(false);
    }
  };

  // Monitor Operations
  const handleCreateMonitor = async (e: React.FormEvent) => {
    e.preventDefault();
    setMonitorError(null);

    try {
      const response = await apiFetch<Monitor>('/api/v1/monitors', {
        method: 'POST',
        bodyData: {
          name: newMonitorName,
          url: newMonitorUrl,
          method: newMonitorMethod,
          checkInterval: newMonitorInterval,
          expectedStatusCode: newMonitorExpectedCode,
          expectedResponseTime: newMonitorExpectedTime,
          headers: newMonitorHeaders
        }
      });
      setMonitors((prev) => [...prev, response]);
      setShowCreateModal(false);
      
      // Reset fields
      setNewMonitorName('');
      setNewMonitorUrl('');
      setNewMonitorMethod('GET');
      setNewMonitorInterval(30);
      setNewMonitorExpectedCode(200);
      setNewMonitorExpectedTime(1000);
      setNewMonitorHeaders('');
    } catch (err: any) {
      setMonitorError(err.message || 'Failed to create monitor');
    }
  };

  const handlePauseResume = async (id: number, currentStatus: string) => {
    const action = currentStatus === 'ACTIVE' ? 'pause' : 'resume';
    try {
      const response = await apiFetch<Monitor>(`/api/v1/monitors/${id}/${action}`, {
        method: 'POST'
      });
      setMonitors((prev) => prev.map((m) => m.id === id ? response : m));
    } catch (e) {
      console.error(e);
    }
  };

  const handleDeleteMonitor = async (id: number) => {
    if (!confirm('Are you sure you want to delete this monitor?')) return;
    try {
      await apiFetch<any>(`/api/v1/monitors/${id}`, { method: 'DELETE' });
      setMonitors((prev) => prev.filter((m) => m.id !== id));
    } catch (e) {
      console.error(e);
    }
  };

  const activeOrg = organizations.find((o) => o.id === activeOrgId);

  // Authentication View Render
  if (!isAuthenticated) {
    return (
      <div className="min-h-screen grid grid-cols-1 lg:grid-cols-2 text-zinc-100 bg-[#09090b] font-sans selection:bg-indigo-500 selection:text-white">
        
        {/* Left Side: Brand Panel */}
        <div className="hidden lg:flex flex-col justify-between p-12 bg-radial from-[#121214] to-[#09090b] border-r border-zinc-800 relative overflow-hidden">
          <div className="absolute top-0 right-0 w-80 h-80 bg-indigo-600/10 rounded-full blur-3xl" />
          <div className="absolute bottom-0 left-0 w-80 h-80 bg-cyan-600/10 rounded-full blur-3xl" />
          
          <div className="flex items-center gap-3 relative z-10">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-500 to-cyan-500 flex items-center justify-center shadow-lg shadow-indigo-500/20">
              <Activity className="w-6 h-6 text-white animate-pulse" />
            </div>
            <span className="font-extrabold text-xl tracking-wider bg-gradient-to-r from-white via-zinc-200 to-zinc-400 bg-clip-text text-transparent">PULSEOPS</span>
          </div>

          <div className="relative z-10 my-auto">
            <h1 className="text-5xl font-black tracking-tight leading-tight max-w-lg mb-6">
              Enterprise API Monitoring &amp; <span className="bg-gradient-to-r from-indigo-400 to-cyan-400 bg-clip-text text-transparent">Real-time Observability</span>
            </h1>
            <p className="text-zinc-400 text-lg max-w-md font-light leading-relaxed">
              Monitor response times, availability, distributed traces, and infrastructure health on a consolidated SaaS dashboard powered by Apache Kafka events.
            </p>
          </div>

          <div className="flex items-center gap-3 text-sm text-zinc-500 relative z-10">
            <Shield className="w-4 h-4 text-indigo-400" />
            <span>Secured with Spring Security &amp; JWT RBAC</span>
          </div>
        </div>

        {/* Right Side: Authentication forms */}
        <div className="flex items-center justify-center p-8 bg-[#0b0b0d]">
          <div className="w-full max-w-md flex flex-col gap-8 glass-card p-10 rounded-2xl border border-zinc-800 shadow-2xl relative">
            <div className="flex flex-col gap-2">
              <h2 className="text-3xl font-bold tracking-tight">
                {isLoginView ? 'Welcome back' : 'Register Organization'}
              </h2>
              <p className="text-zinc-400 text-sm">
                {isLoginView ? 'Enter credentials to access your dashboard' : 'Create an admin account and monitor workspace'}
              </p>
            </div>

            {authError && (
              <div className="p-4 bg-red-500/10 border border-red-500/30 rounded-xl flex items-center gap-3 text-red-400 text-sm">
                <AlertTriangle className="w-5 h-5 flex-shrink-0" />
                <span>{authError}</span>
              </div>
            )}

            {authSuccess && (
              <div className="p-4 bg-emerald-500/10 border border-emerald-500/30 rounded-xl flex items-center gap-3 text-emerald-400 text-sm">
                <CheckCircle className="w-5 h-5 flex-shrink-0" />
                <span>{authSuccess}</span>
              </div>
            )}

            <form onSubmit={handleAuthSubmit} className="flex flex-col gap-4">
              {!isLoginView && (
                <div className="grid grid-cols-2 gap-4">
                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-semibold uppercase tracking-wider text-zinc-400">First Name</label>
                    <input 
                      type="text" 
                      required 
                      value={authFirstName}
                      onChange={(e) => setAuthFirstName(e.target.value)}
                      placeholder="Jane"
                      className="bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:outline-none focus:border-indigo-500 transition" 
                    />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-semibold uppercase tracking-wider text-zinc-400">Last Name</label>
                    <input 
                      type="text" 
                      required
                      value={authLastName}
                      onChange={(e) => setAuthLastName(e.target.value)}
                      placeholder="Doe"
                      className="bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:outline-none focus:border-indigo-500 transition" 
                    />
                  </div>
                </div>
              )}

              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-semibold uppercase tracking-wider text-zinc-400 font-medium">Email Address</label>
                <input 
                  type="email" 
                  required 
                  value={authEmail}
                  onChange={(e) => setAuthEmail(e.target.value)}
                  placeholder="jane.doe@company.com" 
                  className="bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:outline-none focus:border-indigo-500 transition"
                />
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-semibold uppercase tracking-wider text-zinc-400 font-medium">Password</label>
                <input 
                  type="password" 
                  required 
                  value={authPassword}
                  onChange={(e) => setAuthPassword(e.target.value)}
                  placeholder="••••••••" 
                  className="bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:outline-none focus:border-indigo-500 transition"
                />
              </div>

              {!isLoginView && (
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-semibold uppercase tracking-wider text-zinc-400">Organization Name</label>
                  <input 
                    type="text" 
                    required 
                    value={authOrgName}
                    onChange={(e) => setAuthOrgName(e.target.value)}
                    placeholder="Acme Cloud Inc" 
                    className="bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:outline-none focus:border-indigo-500 transition"
                  />
                </div>
              )}

              <button 
                type="submit" 
                disabled={loading}
                className="mt-2 bg-gradient-to-r from-indigo-500 to-cyan-500 text-white font-semibold rounded-xl py-3 text-sm hover:opacity-90 transition flex items-center justify-center gap-2 cursor-pointer shadow-lg shadow-indigo-500/20"
              >
                {loading ? <RefreshCw className="w-5 h-5 animate-spin" /> : (
                  <>
                    <span>{isLoginView ? 'Sign In' : 'Register Now'}</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </form>

            <div className="flex justify-center border-t border-zinc-800 pt-6">
              <button 
                type="button"
                onClick={() => setIsLoginView(!isLoginView)}
                className="text-indigo-400 hover:text-indigo-300 text-xs font-semibold tracking-wide cursor-pointer uppercase transition"
              >
                {isLoginView ? "Don't have an account? Sign Up" : "Already have an account? Log In"}
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Dashboard Application View Render
  const totalMonitors = monitors.length;
  const healthyMonitors = monitors.filter((m) => m.healthStatus === 'UP').length;
  const downMonitors = monitors.filter((m) => m.healthStatus === 'DOWN').length;
  const activeIncidents = incidents.filter((i) => i.status === 'OPEN').length;

  return (
    <div className="min-h-screen text-zinc-100 bg-[#09090b] flex font-sans antialiased">
      
      {/* Sidebar navigation */}
      <aside className="w-64 border-r border-zinc-800 bg-[#0c0c0e] flex flex-col justify-between p-6">
        <div className="flex flex-col gap-8">
          
          {/* Brand header */}
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-indigo-500 to-cyan-500 flex items-center justify-center shadow-lg shadow-indigo-500/20">
              <Activity className="w-5 h-5 text-white animate-pulse" />
            </div>
            <span className="font-extrabold text-sm tracking-wider bg-gradient-to-r from-white via-zinc-200 to-zinc-400 bg-clip-text text-transparent">PULSEOPS</span>
          </div>

          {/* Org Switcher */}
          <div className="flex flex-col gap-1.5">
            <span className="text-[10px] font-bold uppercase tracking-wider text-zinc-500 flex items-center gap-1.5">
              <Building className="w-3 h-3" /> Workspace
            </span>
            <select 
              value={activeOrgId || ''} 
              onChange={(e) => setActiveOrgId(Number(e.target.value))}
              className="bg-zinc-900 border border-zinc-800 text-xs font-semibold rounded-lg px-3 py-2 focus:outline-none focus:border-indigo-500 transition cursor-pointer"
            >
              {organizations.map((org) => (
                <option key={org.id} value={org.id}>{org.name}</option>
              ))}
            </select>
          </div>

          {/* Navigation Links */}
          <nav className="flex flex-col gap-1">
            <button 
              onClick={() => setActiveTab('dashboard')}
              className={`flex items-center gap-3 px-4 py-2.5 rounded-xl text-xs font-semibold transition cursor-pointer ${
                activeTab === 'dashboard' 
                  ? 'bg-zinc-800 text-white border border-zinc-700' 
                  : 'text-zinc-400 hover:text-zinc-200 hover:bg-zinc-900/50'
              }`}
            >
              <Activity className="w-4 h-4" />
              <span>Metrics Dashboard</span>
            </button>
            
            <button 
              onClick={() => setActiveTab('monitors')}
              className={`flex items-center gap-3 px-4 py-2.5 rounded-xl text-xs font-semibold transition cursor-pointer ${
                activeTab === 'monitors' 
                  ? 'bg-zinc-800 text-white border border-zinc-700' 
                  : 'text-zinc-400 hover:text-zinc-200 hover:bg-zinc-900/50'
              }`}
            >
              <Server className="w-4 h-4" />
              <span>API Monitors ({totalMonitors})</span>
            </button>

            <button 
              onClick={() => setActiveTab('incidents')}
              className={`flex items-center gap-3 px-4 py-2.5 rounded-xl text-xs font-semibold transition cursor-pointer ${
                activeTab === 'incidents' 
                  ? 'bg-zinc-800 text-white border border-zinc-700' 
                  : 'text-zinc-400 hover:text-zinc-200 hover:bg-zinc-900/50'
              }`}
            >
              <AlertTriangle className="w-4 h-4" />
              <span>Incident Log ({activeIncidents})</span>
            </button>
          </nav>
        </div>

        {/* User profile / Log out */}
        <div className="border-t border-zinc-800 pt-6 flex flex-col gap-3">
          <div className="flex items-center gap-3 px-2">
            <div className="w-8 h-8 rounded-full bg-indigo-600/30 flex items-center justify-center border border-indigo-500/20">
              <User className="w-4 h-4 text-indigo-300" />
            </div>
            <div className="flex flex-col min-w-0">
              <span className="text-xs font-semibold truncate text-zinc-200">{user?.email}</span>
              <span className="text-[9px] font-bold text-zinc-500 uppercase tracking-wider">{user?.roles[0]}</span>
            </div>
          </div>
          <button 
            onClick={() => clearSession()}
            className="flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold text-red-400 hover:bg-red-500/10 hover:text-red-300 transition cursor-pointer w-full text-left"
          >
            <LogOut className="w-4 h-4" />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>

      {/* Main Panel Content */}
      <main className="flex-1 flex flex-col min-w-0 bg-[#070708] overflow-y-auto">
        
        {/* Top navbar */}
        <header className="h-16 border-b border-zinc-800 px-8 flex items-center justify-between bg-[#0b0b0d]">
          <div className="flex items-center gap-4">
            <h1 className="text-lg font-bold tracking-tight capitalize">{activeTab} Overview</h1>
            <span className="text-xs text-zinc-500">/ {activeOrg?.name}</span>
          </div>

          <div className="flex items-center gap-4">
            {/* Live indicator */}
            <div className="flex items-center gap-2 px-3 py-1 bg-zinc-900 border border-zinc-800 rounded-full">
              <span className={`w-2 h-2 rounded-full ${isConnected ? 'bg-emerald-500 animate-ping' : 'bg-red-500'}`} />
              <span className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider">
                {isConnected ? 'LIVE STREAM' : 'OFFLINE'}
              </span>
            </div>
          </div>
        </header>

        {/* Tab Components */}
        <div className="p-8 flex flex-col gap-8 max-w-7xl w-full mx-auto">
          
          {activeTab === 'dashboard' && (
            <>
              {/* Widgets Stats Row */}
              <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                <div className="glass-card p-6 rounded-2xl flex items-center justify-between">
                  <div className="flex flex-col gap-1">
                    <span className="text-[10px] font-bold text-zinc-500 uppercase tracking-wider">Total API Monitors</span>
                    <span className="text-3xl font-black">{totalMonitors}</span>
                  </div>
                  <Server className="w-8 h-8 text-zinc-600" />
                </div>

                <div className="glass-card p-6 rounded-2xl flex items-center justify-between border-l-2 border-l-emerald-500">
                  <div className="flex flex-col gap-1">
                    <span className="text-[10px] font-bold text-zinc-500 uppercase tracking-wider">Healthy (UP)</span>
                    <span className="text-3xl font-black text-emerald-400">{healthyMonitors}</span>
                  </div>
                  <CheckCircle className="w-8 h-8 text-emerald-500/40" />
                </div>

                <div className="glass-card p-6 rounded-2xl flex items-center justify-between border-l-2 border-l-red-500">
                  <div className="flex flex-col gap-1">
                    <span className="text-[10px] font-bold text-zinc-500 uppercase tracking-wider">Failing (DOWN)</span>
                    <span className="text-3xl font-black text-red-400">{downMonitors}</span>
                  </div>
                  <AlertTriangle className="w-8 h-8 text-red-500/40" />
                </div>

                <div className="glass-card p-6 rounded-2xl flex items-center justify-between border-l-2 border-l-amber-500">
                  <div className="flex flex-col gap-1">
                    <span className="text-[10px] font-bold text-zinc-500 uppercase tracking-wider">Active Incidents</span>
                    <span className="text-3xl font-black text-amber-400">{activeIncidents}</span>
                  </div>
                  <Bell className="w-8 h-8 text-amber-500/40" />
                </div>
              </div>

              {/* Chart and activity feed */}
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                
                {/* Real-time Response Time Chart */}
                <div className="lg:col-span-2 glass-card p-6 rounded-2xl flex flex-col gap-6">
                  <div className="flex justify-between items-center">
                    <h3 className="font-bold text-sm tracking-wide">Live Response Time Latency (ms)</h3>
                    <span className="text-[10px] bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 px-2 py-0.5 rounded-full font-bold">REALTIME</span>
                  </div>
                  <div className="h-80 w-full">
                    {latencyData.length > 0 ? (
                      <ResponsiveContainer width="100%" height="100%">
                        <LineChart data={latencyData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                          <CartesianGrid strokeDasharray="3 3" stroke="#27272a" />
                          <XAxis dataKey="time" stroke="#a1a1aa" fontSize={10} />
                          <YAxis stroke="#a1a1aa" fontSize={10} />
                          <Tooltip contentStyle={{ backgroundColor: '#18181b', borderColor: '#27272a', borderRadius: '8px', color: '#fff', fontSize: 12 }} />
                          <Legend wrapperStyle={{ fontSize: 11 }} />
                          {monitors.map((m, idx) => (
                            <Line 
                              key={m.id}
                              type="monotone" 
                              dataKey={m.name} 
                              stroke={idx === 0 ? '#6366f1' : idx === 1 ? '#06b6d4' : '#10b981'} 
                              activeDot={{ r: 5 }} 
                              strokeWidth={2}
                              dot={false}
                            />
                          ))}
                        </LineChart>
                      </ResponsiveContainer>
                    ) : (
                      <div className="h-full flex items-center justify-center text-zinc-500 text-xs">
                        Awaiting data check events...
                      </div>
                    )}
                  </div>
                </div>

                {/* Kafka / WebSocket Event Stream Log */}
                <div className="glass-card p-6 rounded-2xl flex flex-col gap-6 h-[400px]">
                  <div className="flex justify-between items-center">
                    <h3 className="font-bold text-sm tracking-wide">Kafka Event Stream</h3>
                    <span className="text-[9px] bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 px-2 py-0.5 rounded-full font-bold uppercase">WebSockets</span>
                  </div>

                  <div className="flex-1 overflow-y-auto flex flex-col gap-3 pr-2 scrollbar-thin">
                    {wsLogs.length === 0 ? (
                      <div className="h-full flex flex-col items-center justify-center text-zinc-600 gap-2">
                        <Zap className="w-5 h-5 animate-pulse" />
                        <span className="text-xs">Listening for Kafka topics...</span>
                      </div>
                    ) : (
                      wsLogs.map((log) => (
                        <div key={log.id} className="p-3 bg-zinc-950 border border-zinc-900 rounded-xl flex flex-col gap-1.5">
                          <div className="flex items-center justify-between text-[9px] font-bold">
                            <span className={
                              log.eventType === 'HEALTH_CHECK' 
                                ? 'text-indigo-400' 
                                : log.eventType === 'INCIDENT' 
                                ? 'text-amber-400' 
                                : 'text-rose-400'
                            }>
                              {log.eventType}
                            </span>
                            <span className="text-zinc-600">{log.timestamp}</span>
                          </div>
                          
                          <span className="text-xs font-mono break-all text-zinc-300">
                            {log.eventType === 'HEALTH_CHECK' && (
                              `Check ran for ${log.data.name}. Status: ${log.data.status} (${log.data.responseTimeMs}ms)`
                            )}
                            {log.eventType === 'INCIDENT' && (
                              `${log.data.title} [${log.data.status}]`
                            )}
                            {log.eventType === 'ALERT' && (
                              log.data.message
                            )}
                          </span>
                        </div>
                      ))
                    )}
                  </div>
                </div>

              </div>
            </>
          )}

          {activeTab === 'monitors' && (
            <div className="flex flex-col gap-6">
              
              {/* Monitors controller bar */}
              <div className="flex justify-between items-center">
                <h3 className="font-bold text-base tracking-tight">Active API &amp; Microservice Monitors</h3>
                <button 
                  onClick={() => setShowCreateModal(true)}
                  className="bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs px-4 py-2.5 rounded-xl flex items-center gap-2 cursor-pointer shadow-lg shadow-indigo-600/20 transition"
                >
                  <Plus className="w-4 h-4" /> Add Monitor
                </button>
              </div>

              {/* Monitors Table */}
              <div className="glass-card rounded-2xl overflow-hidden">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-zinc-800 bg-zinc-900/50 text-[10px] font-bold uppercase tracking-wider text-zinc-400">
                      <th className="p-4">Name</th>
                      <th className="p-4">Details</th>
                      <th className="p-4">Interval</th>
                      <th className="p-4">Target Expectations</th>
                      <th className="p-4">Uptime Health</th>
                      <th className="p-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-800">
                    {monitors.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="p-8 text-center text-zinc-500 text-xs">
                          No monitors configured. Click "Add Monitor" to get started.
                        </td>
                      </tr>
                    ) : (
                      monitors.map((monitor) => (
                        <tr key={monitor.id} className="text-xs hover:bg-zinc-900/20 transition">
                          <td className="p-4">
                            <div className="flex flex-col gap-1">
                              <span className="font-semibold text-zinc-200">{monitor.name}</span>
                              <span className="text-[10px] font-mono text-zinc-500 bg-zinc-900 border border-zinc-800 px-2 py-0.5 rounded w-max">
                                {monitor.type}
                              </span>
                            </div>
                          </td>
                          <td className="p-4">
                            <div className="flex items-center gap-2">
                              <span className="font-bold text-[9px] uppercase px-2 py-0.5 bg-zinc-800 text-zinc-300 rounded font-mono">
                                {monitor.method}
                              </span>
                              <span className="truncate max-w-xs font-mono text-zinc-400">{monitor.url}</span>
                            </div>
                          </td>
                          <td className="p-4 font-mono text-zinc-300">{monitor.checkInterval}s</td>
                          <td className="p-4 font-mono text-zinc-400 text-[10px]">
                            Code: {monitor.expectedStatusCode} | Latency: &lt;{monitor.expectedResponseTime}ms
                          </td>
                          <td className="p-4">
                            <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                              monitor.status === 'PAUSED'
                                ? 'bg-zinc-900 border border-zinc-800 text-zinc-500'
                                : monitor.healthStatus === 'UP' 
                                ? 'bg-emerald-500/10 border border-emerald-500/20 text-emerald-400' 
                                : 'bg-red-500/10 border border-red-500/20 text-red-400'
                            }`}>
                              <span className={`w-1.5 h-1.5 rounded-full ${
                                monitor.status === 'PAUSED' ? 'bg-zinc-500' : monitor.healthStatus === 'UP' ? 'bg-emerald-400' : 'bg-red-400'
                              }`} />
                              {monitor.status === 'PAUSED' ? 'PAUSED' : monitor.healthStatus}
                            </span>
                          </td>
                          <td className="p-4 text-right">
                            <div className="flex items-center justify-end gap-2">
                              <button 
                                onClick={() => handlePauseResume(monitor.id, monitor.status)}
                                title={monitor.status === 'ACTIVE' ? 'Pause monitor' : 'Resume monitor'}
                                className="p-2 hover:bg-zinc-800 rounded-lg text-zinc-400 hover:text-zinc-200 transition cursor-pointer"
                              >
                                {monitor.status === 'ACTIVE' ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
                              </button>
                              <button 
                                onClick={() => handleDeleteMonitor(monitor.id)}
                                title="Delete monitor"
                                className="p-2 hover:bg-red-500/10 rounded-lg text-zinc-400 hover:text-red-400 transition cursor-pointer"
                              >
                                <Trash2 className="w-4 h-4" />
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {activeTab === 'incidents' && (
            <div className="flex flex-col gap-6">
              <h3 className="font-bold text-base tracking-tight">System Incident Logs</h3>

              <div className="glass-card rounded-2xl overflow-hidden">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-zinc-800 bg-zinc-900/50 text-[10px] font-bold uppercase tracking-wider text-zinc-400">
                      <th className="p-4">Incident Details</th>
                      <th className="p-4">Severity</th>
                      <th className="p-4">Status</th>
                      <th className="p-4">Started At</th>
                      <th className="p-4">Resolved At</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-800">
                    {incidents.length === 0 ? (
                      <tr>
                        <td colSpan={5} className="p-8 text-center text-zinc-500 text-xs">
                          No incidents registered. Your services are operating normally.
                        </td>
                      </tr>
                    ) : (
                      incidents.map((incident) => (
                        <tr key={incident.id} className="text-xs hover:bg-zinc-900/20 transition">
                          <td className="p-4">
                            <div className="flex flex-col gap-1.5">
                              <span className="font-semibold text-zinc-200">#{incident.id} - {incident.title}</span>
                              <span className="text-[10px] text-zinc-400 font-mono line-clamp-1">{incident.description}</span>
                            </div>
                          </td>
                          <td className="p-4">
                            <span className="text-[10px] font-bold uppercase text-rose-400 bg-rose-500/10 border border-rose-500/20 px-2 py-0.5 rounded">
                              {incident.severity}
                            </span>
                          </td>
                          <td className="p-4">
                            <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                              incident.status === 'RESOLVED' 
                                ? 'bg-emerald-500/10 border border-emerald-500/20 text-emerald-400' 
                                : 'bg-amber-500/10 border border-amber-500/20 text-amber-400 animate-pulse'
                            }`}>
                              {incident.status}
                            </span>
                          </td>
                          <td className="p-4 font-mono text-zinc-400">{new Date(incident.startedAt).toLocaleString()}</td>
                          <td className="p-4 font-mono text-zinc-500">
                            {incident.resolvedAt ? new Date(incident.resolvedAt).toLocaleString() : '--'}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

        </div>

        {/* Create Monitor Modal Pop-up */}
        {showCreateModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-fade-in">
            <div className="w-full max-w-lg bg-[#0e0e11] border border-zinc-800 rounded-2xl p-8 shadow-2xl flex flex-col gap-6">
              <div className="flex justify-between items-center">
                <h3 className="text-lg font-bold tracking-tight">Configure New API Monitor</h3>
                <button 
                  onClick={() => setShowCreateModal(false)}
                  className="text-zinc-500 hover:text-zinc-300 text-xs font-semibold cursor-pointer"
                >
                  Cancel
                </button>
              </div>

              {monitorError && (
                <div className="p-3 bg-red-500/15 border border-red-500/30 text-red-400 text-xs rounded-xl">
                  {monitorError}
                </div>
              )}

              <form onSubmit={handleCreateMonitor} className="grid grid-cols-2 gap-4">
                <div className="col-span-2 flex flex-col gap-1.5">
                  <label className="text-[10px] font-bold uppercase tracking-wider text-zinc-500">Monitor Name</label>
                  <input 
                    type="text" 
                    required 
                    value={newMonitorName}
                    onChange={(e) => setNewMonitorName(e.target.value)}
                    placeholder="Auth Service / Health Check"
                    className="bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs focus:outline-none focus:border-indigo-500 transition" 
                  />
                </div>

                <div className="col-span-2 flex flex-col gap-1.5">
                  <label className="text-[10px] font-bold uppercase tracking-wider text-zinc-500">Endpoint Target URL</label>
                  <input 
                    type="url" 
                    required 
                    value={newMonitorUrl}
                    onChange={(e) => setNewMonitorUrl(e.target.value)}
                    placeholder="https://api.acme.com/health"
                    className="bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs focus:outline-none focus:border-indigo-500 transition" 
                  />
                </div>

                <div className="flex flex-col gap-1.5">
                  <label className="text-[10px] font-bold uppercase tracking-wider text-zinc-500">HTTP Method</label>
                  <select 
                    value={newMonitorMethod}
                    onChange={(e) => setNewMonitorMethod(e.target.value)}
                    className="bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs focus:outline-none focus:border-indigo-500 transition cursor-pointer"
                  >
                    <option value="GET">GET</option>
                    <option value="POST">POST</option>
                    <option value="PUT">PUT</option>
                    <option value="DELETE">DELETE</option>
                  </select>
                </div>

                <div className="flex flex-col gap-1.5">
                  <label className="text-[10px] font-bold uppercase tracking-wider text-zinc-500">Check Interval (Seconds)</label>
                  <input 
                    type="number" 
                    required 
                    min={10}
                    value={newMonitorInterval}
                    onChange={(e) => setNewMonitorInterval(Number(e.target.value))}
                    className="bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs focus:outline-none focus:border-indigo-500 transition" 
                  />
                </div>

                <div className="flex flex-col gap-1.5">
                  <label className="text-[10px] font-bold uppercase tracking-wider text-zinc-500">Expected Code</label>
                  <input 
                    type="number" 
                    required 
                    value={newMonitorExpectedCode}
                    onChange={(e) => setNewMonitorExpectedCode(Number(e.target.value))}
                    className="bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs focus:outline-none focus:border-indigo-500 transition" 
                  />
                </div>

                <div className="flex flex-col gap-1.5">
                  <label className="text-[10px] font-bold uppercase tracking-wider text-zinc-500">Max Acceptable Latency (ms)</label>
                  <input 
                    type="number" 
                    required 
                    value={newMonitorExpectedTime}
                    onChange={(e) => setNewMonitorExpectedTime(Number(e.target.value))}
                    className="bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs focus:outline-none focus:border-indigo-500 transition" 
                  />
                </div>

                <div className="col-span-2 flex flex-col gap-1.5">
                  <label className="text-[10px] font-bold uppercase tracking-wider text-zinc-500">JSON Request Headers (Optional)</label>
                  <textarea 
                    value={newMonitorHeaders}
                    onChange={(e) => setNewMonitorHeaders(e.target.value)}
                    placeholder='{"X-Custom-Header": "value"}'
                    rows={2}
                    className="bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs font-mono focus:outline-none focus:border-indigo-500 transition" 
                  />
                </div>

                <button 
                  type="submit"
                  className="col-span-2 mt-4 bg-gradient-to-r from-indigo-500 to-cyan-500 text-white font-semibold rounded-xl py-3 text-xs hover:opacity-90 transition cursor-pointer"
                >
                  Create &amp; Activate Monitor
                </button>
              </form>
            </div>
          </div>
        )}

      </main>
    </div>
  );
}
