import React, { useState, useEffect, useRef } from 'react';
import Navbar from './components/Navbar';
import AuthModal from './components/AuthModal';
import UploadZone from './components/UploadZone';
import StatsCards from './components/StatsCards';
import DocumentList from './components/DocumentList';
import AnalysisModal from './components/AnalysisModal';
import { getMyDocuments } from './api';
import { Sparkles, ArrowRight, Shield, Cpu, RefreshCw } from 'lucide-react';

export default function App() {
  const [userEmail, setUserEmail] = useState(localStorage.getItem('userEmail') || null);
  const [token, setToken] = useState(localStorage.getItem('token') || null);
  const [isAuthOpen, setIsAuthOpen] = useState(false);
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedDoc, setSelectedDoc] = useState(null);
  const pollingRef = useRef(null);

  const fetchDocs = async () => {
    if (!token) return;
    try {
      setLoading(true);
      const data = await getMyDocuments(0, 50);
      setDocuments(data.content || []);
    } catch (err) {
      if (err.message === 'Unauthorized') {
        handleLogout();
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (token) {
      fetchDocs();
    } else {
      setDocuments([]);
    }
  }, [token]);

  // Polling for processing documents
  useEffect(() => {
    const hasProcessing = documents.some((d) => d.processingStatus === 'PROCESSING');

    if (hasProcessing && token) {
      if (!pollingRef.current) {
        pollingRef.current = setInterval(async () => {
          try {
            const data = await getMyDocuments(0, 50);
            setDocuments(data.content || []);
          } catch {
            // ignore temporary poll errors
          }
        }, 3000);
      }
    } else {
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
    }

    return () => {
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
    };
  }, [documents, token]);

  const handleAuthSuccess = (email, newToken) => {
    setUserEmail(email);
    setToken(newToken);
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userEmail');
    setUserEmail(null);
    setToken(null);
    setDocuments([]);
  };

  const handleUploadSuccess = (newDoc) => {
    setDocuments((prev) => [newDoc, ...prev]);
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col selection:bg-indigo-500 selection:text-white">
      {/* Navbar */}
      <Navbar
        userEmail={userEmail}
        onLogout={handleLogout}
        onOpenAuth={() => setIsAuthOpen(true)}
      />

      {/* Main Content */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        
        {/* Hero Section if not authenticated */}
        {!userEmail && (
          <div className="relative rounded-3xl overflow-hidden border border-slate-800 bg-gradient-to-b from-slate-900 to-slate-950 p-8 sm:p-12 text-center shadow-2xl">
            <div className="absolute inset-0 bg-gradient-to-tr from-indigo-500/10 via-transparent to-violet-500/10 pointer-events-none" />
            
            <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 text-xs font-semibold mb-6">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Microservices-Powered Document Intelligence</span>
            </div>

            <h1 className="font-display text-3xl sm:text-5xl font-extrabold text-white tracking-tight max-w-3xl mx-auto leading-tight">
              Instant Document Extraction, Classification & Risk Scoring
            </h1>

            <p className="text-sm sm:text-base text-slate-400 max-w-2xl mx-auto mt-4 leading-relaxed">
              Upload enterprise contracts, financial statements, and resumes. Extracted via Apache Tika and analyzed asynchronously with OpenAI LLMs.
            </p>

            <div className="mt-8 flex items-center justify-center space-x-4">
              <button
                onClick={() => setIsAuthOpen(true)}
                className="flex items-center space-x-2 px-6 py-3 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-sm rounded-xl shadow-lg shadow-indigo-600/30 transition transform hover:-translate-y-0.5"
              >
                <span>Get Started Now</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>

            {/* Architecture Highlights */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-12 max-w-3xl mx-auto text-left">
              <div className="bg-slate-800/40 border border-slate-800 p-4 rounded-xl">
                <Shield className="w-5 h-5 text-indigo-400 mb-2" />
                <h4 className="font-semibold text-xs text-white">Stateless JWT & RBAC</h4>
                <p className="text-[11px] text-slate-400 mt-0.5">API Gateway perimeter guard with header anti-spoofing.</p>
              </div>

              <div className="bg-slate-800/40 border border-slate-800 p-4 rounded-xl">
                <Cpu className="w-5 h-5 text-emerald-400 mb-2" />
                <h4 className="font-semibold text-xs text-white">Decoupled Async Pipeline</h4>
                <p className="text-[11px] text-slate-400 mt-0.5">Non-blocking text extraction and asynchronous LLM dispatch.</p>
              </div>

              <div className="bg-slate-800/40 border border-slate-800 p-4 rounded-xl">
                <Sparkles className="w-5 h-5 text-violet-400 mb-2" />
                <h4 className="font-semibold text-xs text-white">Structured AI Analysis</h4>
                <p className="text-[11px] text-slate-400 mt-0.5">Executive summaries, automatic categorization & risk ratings.</p>
              </div>
            </div>
          </div>
        )}

        {/* Dashboard when logged in */}
        {userEmail && (
          <>
            {/* Stats Overview */}
            <StatsCards documents={documents} />

            {/* Upload Zone */}
            <UploadZone
              isAuthenticated={Boolean(token)}
              onRequireAuth={() => setIsAuthOpen(true)}
              onUploadSuccess={handleUploadSuccess}
            />

            {/* Documents List */}
            <DocumentList
              documents={documents}
              loading={loading}
              onRefresh={fetchDocs}
              onSelectDocument={(doc) => setSelectedDoc(doc)}
            />
          </>
        )}

      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800/80 py-6 mt-12 bg-slate-950 text-center text-xs text-slate-500">
        <p>AI Document Intelligence Platform • Powered by Spring Boot 3, Spring Cloud Gateway, Consul & OpenAI</p>
      </footer>

      {/* Modals */}
      <AuthModal
        isOpen={isAuthOpen}
        onClose={() => setIsAuthOpen(false)}
        onAuthSuccess={handleAuthSuccess}
      />

      {selectedDoc && (
        <AnalysisModal
          document={selectedDoc}
          onClose={() => setSelectedDoc(null)}
        />
      )}
    </div>
  );
}
