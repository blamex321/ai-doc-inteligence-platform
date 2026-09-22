import React, { useState } from 'react';
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
  Calendar,
  Layers,
  CheckCircle2,
  Clock,
  AlertCircle
} from 'lucide-react';
import { downloadDocument } from '../api';

export default function AnalysisModal({ document, onClose }) {
  const [copied, setCopied] = useState(false);

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
      <div className="relative w-full max-w-2xl bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl overflow-hidden max-h-[90vh] flex flex-col">
        
        {/* Modal Header */}
        <div className="p-6 border-b border-slate-800 flex items-start justify-between bg-slate-800/30">
          <div className="flex items-center space-x-3">
            <div className="w-12 h-12 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 flex items-center justify-center">
              <FileText className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h3 className="font-display text-lg font-bold text-white truncate max-w-md">
                  {document.fileName}
                </h3>
              </div>
              <p className="text-xs text-slate-400 mt-0.5">
                ID: <span className="font-mono text-slate-300">{document.id}</span>
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Scrollable Content */}
        <div className="p-6 overflow-y-auto space-y-6">
          
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
                <span>Executive Summary (LLM Generated)</span>
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
