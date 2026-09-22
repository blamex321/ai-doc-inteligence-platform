import React, { useState, useRef, useEffect } from 'react';
import {
  X,
  FileText,
  Sparkles,
  ShieldAlert,
  ShieldCheck,
  Shield,
  Download,
  Copy,
  Check,
  Tag,
  MessageSquare,
  Send,
  Bot,
  User,
  BookOpen,
  ChevronDown,
  ChevronUp,
  Loader2,
  HelpCircle,
  Clock
} from 'lucide-react';
import { downloadDocument, chatWithDocument } from '../api';

export default function AnalysisModal({ document, onClose }) {
  const [activeTab, setActiveTab] = useState('overview');
  const [copied, setCopied] = useState(false);

  // Chat State
  const [messages, setMessages] = useState([
    {
      id: 'welcome',
      sender: 'ai',
      text: `Hello! I have indexed "${document?.fileName}". Ask any question, and I will answer grounded directly in the document's content with verifiable citations.`,
      sources: [],
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    }
  ]);
  const [inputQuestion, setInputQuestion] = useState('');
  const [isQuerying, setIsQuerying] = useState(false);
  const [expandedSources, setExpandedSources] = useState({});
  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    if (activeTab === 'chat') {
      scrollToBottom();
    }
  }, [messages, activeTab]);

  if (!document) return null;

  const handleCopy = () => {
    if (document.summary) {
      navigator.clipboard.writeText(document.summary);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const handleDownload = async () => {
    try {
      await downloadDocument(document.id, document.fileName);
    } catch (err) {
      alert('Download error: ' + err.message);
    }
  };

  const toggleSource = (msgId) => {
    setExpandedSources(prev => ({
      ...prev,
      [msgId]: !prev[msgId]
    }));
  };

  const handleSendMessage = async (queryText) => {
    const text = (queryText || inputQuestion).trim();
    if (!text || isQuerying) return;

    const userMsg = {
      id: 'user-' + Date.now(),
      sender: 'user',
      text,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setMessages(prev => [...prev, userMsg]);
    setInputQuestion('');
    setIsQuerying(true);

    try {
      const response = await chatWithDocument(document.id, text);
      const aiMsg = {
        id: 'ai-' + Date.now(),
        sender: 'ai',
        text: response.answer || 'No answer generated.',
        sources: response.relevantSources || [],
        model: response.model,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      };
      setMessages(prev => [...prev, aiMsg]);
    } catch (err) {
      const errorMsg = {
        id: 'err-' + Date.now(),
        sender: 'ai',
        text: `Error processing question: ${err.message}. Please try again.`,
        sources: [],
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      };
      setMessages(prev => [...prev, errorMsg]);
    } finally {
      setIsQuerying(false);
    }
  };

  const quickPrompts = [
    "What is the main topic and purpose of this document?",
    "What are the key obligations, conditions, or action items?",
    "Are there any financial figures, dates, or deadlines mentioned?",
    "Highlight any potential risks, liabilities, or exceptions."
  ];

  const renderRiskScoreCard = (score) => {
    const s = score?.toLowerCase();
    let bg = 'bg-slate-800/80 border-slate-700 text-slate-300';
    let icon = <Shield className="w-5 h-5" />;
    let label = 'Low Risk';
    let desc = 'Standard operational or non-sensitive document.';

    if (s === 'high') {
      bg = 'bg-rose-500/10 border-rose-500/30 text-rose-300';
      icon = <ShieldAlert className="w-5 h-5 text-rose-400" />;
      label = 'High Risk Detected';
      desc = 'Contains sensitive agreements, confidential clauses, or significant liability.';
    } else if (s === 'medium') {
      bg = 'bg-amber-500/10 border-amber-500/30 text-amber-300';
      icon = <Shield className="w-5 h-5 text-amber-400" />;
      label = 'Medium Risk';
      desc = 'Contains financial data, invoices, or business operational metrics.';
    } else if (s === 'low') {
      bg = 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300';
      icon = <ShieldCheck className="w-5 h-5 text-emerald-400" />;
      label = 'Low Risk';
      desc = 'Safe document, general information, or public resume profile.';
    }

    return (
      <div className={`p-4 rounded-xl border ${bg} flex items-start space-x-3`}>
        <div className="shrink-0 mt-0.5">{icon}</div>
        <div>
          <div className="font-semibold text-sm">{label}</div>
          <p className="text-xs opacity-80 mt-0.5">{desc}</p>
        </div>
      </div>
    );
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/75 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="relative w-full max-w-3xl bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl overflow-hidden max-h-[92vh] flex flex-col">
        
        {/* Modal Header */}
        <div className="p-5 border-b border-slate-800 flex items-start justify-between bg-slate-800/40">
          <div className="flex items-center space-x-3 truncate">
            <div className="w-11 h-11 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 flex items-center justify-center shrink-0">
              <FileText className="w-6 h-6" />
            </div>
            <div className="truncate">
              <h3 className="font-display text-base sm:text-lg font-bold text-white truncate">
                {document.fileName}
              </h3>
              <p className="text-xs text-slate-400 mt-0.5 truncate">
                ID: <span className="font-mono text-slate-300">{document.id}</span>
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition shrink-0 ml-3"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Switcher */}
        <div className="flex border-b border-slate-800 bg-slate-900/60 px-6 pt-2">
          <button
            onClick={() => setActiveTab('overview')}
            className={`flex items-center space-x-2 py-3 px-4 border-b-2 font-medium text-xs sm:text-sm transition ${
              activeTab === 'overview'
                ? 'border-indigo-500 text-indigo-400 font-semibold'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Sparkles className="w-4 h-4" />
            <span>Analysis & Overview</span>
          </button>

          <button
            onClick={() => setActiveTab('chat')}
            className={`flex items-center space-x-2 py-3 px-4 border-b-2 font-medium text-xs sm:text-sm transition ${
              activeTab === 'chat'
                ? 'border-indigo-500 text-indigo-400 font-semibold'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <MessageSquare className="w-4 h-4" />
            <span>Chat with Document</span>
            <span className="ml-1.5 px-1.5 py-0.5 text-[10px] uppercase font-bold rounded-md bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
              RAG
            </span>
          </button>
        </div>

        {/* Tab 1: Overview & Analysis */}
        {activeTab === 'overview' && (
          <div className="p-6 overflow-y-auto space-y-6 flex-1">
            
            {/* Metadata Badges */}
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              <div className="bg-slate-800/40 border border-slate-800 p-3 rounded-xl">
                <span className="text-[11px] text-slate-500 uppercase tracking-wider font-semibold block">
                  Classification
                </span>
                <span className="text-sm font-semibold text-indigo-400 mt-0.5 block">
                  {document.classification || 'General'}
                </span>
              </div>

              <div className="bg-slate-800/40 border border-slate-800 p-3 rounded-xl">
                <span className="text-[11px] text-slate-500 uppercase tracking-wider font-semibold block">
                  Pipeline Status
                </span>
                <span className="text-sm font-semibold text-emerald-400 mt-0.5 block">
                  {document.processingStatus || 'COMPLETED'}
                </span>
              </div>

              <div className="bg-slate-800/40 border border-slate-800 p-3 rounded-xl col-span-2 sm:col-span-1">
                <span className="text-[11px] text-slate-500 uppercase tracking-wider font-semibold block">
                  Ingested By
                </span>
                <span className="text-xs font-medium text-slate-300 mt-1 block truncate">
                  {document.uploadedBy || 'user'}
                </span>
              </div>
            </div>

            {/* Risk Card */}
            <div>
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2 flex items-center space-x-1.5">
                <Shield className="w-3.5 h-3.5" />
                <span>Automated Risk Assessment</span>
              </h4>
              {renderRiskScoreCard(document.riskScore)}
            </div>

            {/* AI Executive Summary */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center space-x-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
                  <span>Executive Summary (LLM Grounded)</span>
                </h4>
                <button
                  onClick={handleCopy}
                  className="inline-flex items-center space-x-1 text-xs text-indigo-400 hover:text-indigo-300 transition"
                >
                  {copied ? (
                    <>
                      <Check className="w-3.5 h-3.5 text-emerald-400" />
                      <span className="text-emerald-400">Copied</span>
                    </>
                  ) : (
                    <>
                      <Copy className="w-3.5 h-3.5" />
                      <span>Copy</span>
                    </>
                  )}
                </button>
              </div>

              <div className="p-4 rounded-xl bg-slate-800/60 border border-slate-700/60 text-slate-200 text-sm leading-relaxed whitespace-pre-wrap font-sans">
                {document.summary || 'Summary is currently being generated by OpenAI service...'}
              </div>
            </div>

            {/* Keywords / Concepts */}
            {document.keywords && document.keywords.length > 0 && (
              <div>
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2.5 flex items-center space-x-1.5">
                  <Tag className="w-3.5 h-3.5" />
                  <span>Identified Concepts & Keywords</span>
                </h4>
                <div className="flex flex-wrap gap-2">
                  {document.keywords.map((kw, idx) => (
                    <span
                      key={idx}
                      className="px-3 py-1 text-xs font-medium rounded-full bg-indigo-500/10 text-indigo-300 border border-indigo-500/20"
                    >
                      #{kw}
                    </span>
                  ))}
                </div>
              </div>
            )}

          </div>
        )}

        {/* Tab 2: Chat with Document (RAG) */}
        {activeTab === 'chat' && (
          <div className="flex flex-col flex-1 min-h-[420px] max-h-[560px]">
            
            {/* Chat Messages */}
            <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-4">
              {messages.map((msg) => (
                <div
                  key={msg.id}
                  className={`flex flex-col ${msg.sender === 'user' ? 'items-end' : 'items-start'}`}
                >
                  <div
                    className={`flex items-start space-x-2.5 max-w-[85%] ${
                      msg.sender === 'user' ? 'flex-row-reverse space-x-reverse' : 'flex-row'
                    }`}
                  >
                    {/* Avatar */}
                    <div
                      className={`w-7 h-7 rounded-lg flex items-center justify-center shrink-0 text-xs ${
                        msg.sender === 'user'
                          ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30'
                          : 'bg-slate-800 border border-slate-700 text-indigo-400'
                      }`}
                    >
                      {msg.sender === 'user' ? <User className="w-4 h-4" /> : <Bot className="w-4 h-4" />}
                    </div>

                    {/* Message Bubble */}
                    <div
                      className={`p-3.5 rounded-2xl text-xs sm:text-sm leading-relaxed ${
                        msg.sender === 'user'
                          ? 'bg-indigo-600 text-white rounded-tr-none'
                          : 'bg-slate-800/80 border border-slate-700/80 text-slate-200 rounded-tl-none'
                      }`}
                    >
                      {msg.sender === 'ai' && msg.model && (
                        <div className="flex items-center space-x-1.5 mb-2 pb-1.5 border-b border-slate-700/40 text-[10px]">
                          <Sparkles className="w-3 h-3 text-indigo-400" />
                          <span className="font-semibold text-slate-400 uppercase tracking-wider">
                            Engine:
                          </span>
                          <span className={`px-1.5 py-0.5 rounded font-mono text-[10px] ${
                            msg.model.includes('gpt') 
                              ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' 
                              : 'bg-amber-500/10 text-amber-300 border border-amber-500/20'
                          }`}>
                            {msg.model.includes('gpt') ? 'OpenAI GPT-4o-mini' : 'Local Semantic RAG Engine'}
                          </span>
                        </div>
                      )}

                      <div className="whitespace-pre-wrap font-sans">{msg.text}</div>

                      {/* Source Citations Drawer for AI replies */}
                      {msg.sources && msg.sources.length > 0 && (
                        <div className="mt-3 pt-2.5 border-t border-slate-700/60">
                          <button
                            onClick={() => toggleSource(msg.id)}
                            className="flex items-center space-x-1.5 text-[11px] font-medium text-indigo-300 hover:text-indigo-200 transition"
                          >
                            <BookOpen className="w-3.5 h-3.5" />
                            <span>
                              {expandedSources[msg.id]
                                ? 'Hide Grounded Sources'
                                : `View Grounded Citations (${msg.sources.length} sources)`}
                            </span>
                            {expandedSources[msg.id] ? (
                              <ChevronUp className="w-3.5 h-3.5" />
                            ) : (
                              <ChevronDown className="w-3.5 h-3.5" />
                            )}
                          </button>

                          {expandedSources[msg.id] && (
                            <div className="mt-2 space-y-2 animate-in fade-in duration-150">
                              {msg.sources.map((src, sIdx) => (
                                <div
                                  key={sIdx}
                                  className="p-2.5 rounded-lg bg-slate-900/90 border border-slate-800 text-[11px] text-slate-300 font-mono leading-relaxed"
                                >
                                  <span className="text-indigo-400 font-semibold block mb-1">
                                    [Source Reference #{sIdx + 1}]
                                  </span>
                                  "{src}"
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  </div>

                  <span className="text-[10px] text-slate-500 mt-1 px-1">
                    {msg.timestamp}
                  </span>
                </div>
              ))}

              {isQuerying && (
                <div className="flex items-center space-x-2 text-slate-400 text-xs pl-2">
                  <Loader2 className="w-4 h-4 animate-spin text-indigo-400" />
                  <span>Searching document chunks and generating grounded answer...</span>
                </div>
              )}

              <div ref={messagesEndRef} />
            </div>

            {/* Quick Prompt Chips */}
            <div className="px-4 sm:px-6 py-2 border-t border-slate-800/80 bg-slate-950/40">
              <div className="flex items-center space-x-1.5 text-[11px] text-slate-400 mb-1.5">
                <HelpCircle className="w-3 h-3 text-indigo-400" />
                <span>Suggested Questions:</span>
              </div>
              <div className="flex flex-wrap gap-1.5">
                {quickPrompts.map((prompt, pIdx) => (
                  <button
                    key={pIdx}
                    onClick={() => handleSendMessage(prompt)}
                    disabled={isQuerying}
                    className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-800/80 hover:bg-slate-700 border border-slate-700/60 text-slate-300 hover:text-white transition disabled:opacity-50 text-left truncate max-w-full sm:max-w-xs"
                  >
                    {prompt}
                  </button>
                ))}
              </div>
            </div>

            {/* Chat Input */}
            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleSendMessage();
              }}
              className="p-3 sm:p-4 border-t border-slate-800 bg-slate-900 flex items-center space-x-2"
            >
              <input
                type="text"
                value={inputQuestion}
                onChange={(e) => setInputQuestion(e.target.value)}
                placeholder="Ask anything about this document..."
                disabled={isQuerying}
                className="flex-1 bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition disabled:opacity-50"
              />
              <button
                type="submit"
                disabled={!inputQuestion.trim() || isQuerying}
                className="p-2.5 rounded-xl bg-indigo-600 text-white hover:bg-indigo-500 transition disabled:opacity-50 shadow-md shadow-indigo-600/30 shrink-0"
              >
                {isQuerying ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <Send className="w-4 h-4" />
                )}
              </button>
            </form>

          </div>
        )}

        {/* Modal Footer */}
        <div className="p-4 border-t border-slate-800 bg-slate-800/30 flex items-center justify-between">
          <button
            onClick={onClose}
            className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white bg-slate-800 hover:bg-slate-700 rounded-xl transition"
          >
            Close
          </button>

          <button
            onClick={handleDownload}
            className="flex items-center space-x-2 px-4 py-2 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-500 rounded-xl shadow-lg shadow-indigo-600/30 transition transform hover:-translate-y-0.5"
          >
            <Download className="w-4 h-4" />
            <span>Download Original File</span>
          </button>
        </div>

      </div>
    </div>
  );
}
