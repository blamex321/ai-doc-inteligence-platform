import React, { useState } from 'react';
import {
  FileText,
  Search,
  Download,
  Trash2,
  ExternalLink,
  Loader2,
  AlertCircle,
  CheckCircle2,
  Clock,
  ShieldAlert,
  ShieldCheck,
  Shield
} from 'lucide-react';
import { downloadDocument, deleteDocument } from '../api';

export default function DocumentList({
  documents = [],
  onSelectDocument,
  onRefresh,
  loading,
}) {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [deletingId, setDeletingId] = useState(null);

  const filteredDocs = documents.filter((doc) => {
    const matchesSearch =
      (doc.fileName && doc.fileName.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (doc.classification && doc.classification.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (doc.summary && doc.summary.toLowerCase().includes(searchTerm.toLowerCase()));

    const matchesStatus =
      statusFilter === 'ALL' || doc.processingStatus === statusFilter;

    return matchesSearch && matchesStatus;
  });

  const handleDownload = async (e, doc) => {
    e.stopPropagation();
    try {
      await downloadDocument(doc.id, doc.fileName);
    } catch (err) {
      alert('Failed to download document: ' + err.message);
    }
  };

  const handleDelete = async (e, docId) => {
    e.stopPropagation();
    if (!window.confirm('Are you sure you want to delete this document?')) return;

    setDeletingId(docId);
    try {
      await deleteDocument(docId);
      onRefresh();
    } catch (err) {
      alert('Failed to delete document: ' + err.message);
    } finally {
      setDeletingId(null);
    }
  };

  const renderStatusBadge = (status) => {
    switch (status) {
      case 'COMPLETED':
        return (
          <span className="inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <CheckCircle2 className="w-3 h-3" />
            <span>Ready</span>
          </span>
        );
      case 'PROCESSING':
        return (
          <span className="inline-flex items-center space-x-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-sky-500/10 text-sky-400 border border-sky-500/20 animate-pulse">
            <Loader2 className="w-3 h-3 animate-spin" />
            <span>Analyzing...</span>
          </span>
        );
      case 'FAILED':
        return (
          <span className="inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-rose-500/10 text-rose-400 border border-rose-500/20">
            <AlertCircle className="w-3 h-3" />
            <span>Failed</span>
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-800 text-slate-400">
            <Clock className="w-3 h-3" />
            <span>{status}</span>
          </span>
        );
    }
  };

  const renderRiskBadge = (score) => {
    switch (score?.toLowerCase()) {
      case 'high':
        return (
          <span className="inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20">
            <ShieldAlert className="w-3 h-3" />
            <span>High Risk</span>
          </span>
        );
      case 'medium':
        return (
          <span className="inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20">
            <Shield className="w-3 h-3" />
            <span>Medium</span>
          </span>
        );
      case 'low':
        return (
          <span className="inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <ShieldCheck className="w-3 h-3" />
            <span>Low Risk</span>
          </span>
        );
      default:
        return <span className="text-slate-500 text-xs">—</span>;
    }
  };

  const formatSize = (bytes) => {
    if (!bytes) return '—';
    return (bytes / 1024 / 1024).toFixed(2) + ' MB';
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '—';
    try {
      return new Date(dateStr).toLocaleString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="bg-slate-900/60 border border-slate-800 rounded-2xl shadow-xl overflow-hidden">
      
      {/* Header & Controls */}
      <div className="p-5 border-b border-slate-800/80 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div>
          <h3 className="font-display text-lg font-bold text-white flex items-center space-x-2">
            <span>Document Repository</span>
            <span className="text-xs px-2 py-0.5 rounded-full bg-slate-800 text-slate-400 font-normal">
              {filteredDocs.length} of {documents.length}
            </span>
          </h3>
          <p className="text-xs text-slate-400">Classified documents with AI risk scoring</p>
        </div>

        {/* Filters */}
        <div className="flex items-center space-x-3 w-full sm:w-auto">
          {/* Search Bar */}
          <div className="relative flex-1 sm:w-60">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
            <input
              type="text"
              placeholder="Search filename or topic..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full bg-slate-800/70 border border-slate-700/60 rounded-xl pl-9 pr-3 py-1.5 text-xs text-white placeholder-slate-400 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
          </div>

          {/* Status Filter */}
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-slate-800/70 border border-slate-700/60 rounded-xl px-3 py-1.5 text-xs text-slate-300 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          >
            <option value="ALL">All Status</option>
            <option value="COMPLETED">Completed</option>
            <option value="PROCESSING">Processing</option>
            <option value="FAILED">Failed</option>
          </select>
        </div>
      </div>

      {/* Table / List */}
      {filteredDocs.length === 0 ? (
        <div className="p-12 text-center">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-slate-800 text-slate-500 mb-3">
            <FileText className="w-6 h-6" />
          </div>
          <p className="text-sm font-semibold text-slate-300">No documents found</p>
          <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
            {searchTerm || statusFilter !== 'ALL'
              ? 'Try adjusting your search criteria or status filter.'
              : 'Upload a PDF, DOCX, or text file above to trigger AI text extraction and classification.'}
          </p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-800 text-[11px] font-semibold text-slate-400 uppercase tracking-wider bg-slate-800/30">
                <th className="py-3 px-5">Document</th>
                <th className="py-3 px-4">Classification</th>
                <th className="py-3 px-4">Risk Level</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Uploaded</th>
                <th className="py-3 px-5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-xs">
              {filteredDocs.map((doc) => (
                <tr
                  key={doc.id}
                  onClick={() => onSelectDocument(doc)}
                  className="hover:bg-slate-800/40 cursor-pointer transition-colors group"
                >
                  {/* File & Name */}
                  <td className="py-3.5 px-5">
                    <div className="flex items-center space-x-3">
                      <div className="w-9 h-9 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 flex items-center justify-center shrink-0">
                        <FileText className="w-4 h-4" />
                      </div>
                      <div className="max-w-xs truncate">
                        <p className="font-semibold text-white group-hover:text-indigo-400 transition-colors truncate">
                          {doc.fileName}
                        </p>
                        <p className="text-[11px] text-slate-500">{formatSize(doc.fileSize)}</p>
                      </div>
                    </div>
                  </td>

                  {/* Classification */}
                  <td className="py-3.5 px-4">
                    {doc.classification ? (
                      <span className="px-2.5 py-0.5 rounded-lg text-xs font-medium bg-slate-800 text-slate-200 border border-slate-700">
                        {doc.classification}
                      </span>
                    ) : (
                      <span className="text-slate-500 italic text-[11px]">Processing...</span>
                    )}
                  </td>

                  {/* Risk Score */}
                  <td className="py-3.5 px-4">{renderRiskBadge(doc.riskScore)}</td>

                  {/* Status */}
                  <td className="py-3.5 px-4">{renderStatusBadge(doc.processingStatus)}</td>

                  {/* Upload Date */}
                  <td className="py-3.5 px-4 text-slate-400 text-[11px] whitespace-nowrap">
                    {formatDate(doc.uploadedAt)}
                  </td>

                  {/* Actions */}
                  <td className="py-3.5 px-5 text-right whitespace-nowrap">
                    <div className="flex items-center justify-end space-x-2">
                      <button
                        title="View Full Analysis"
                        onClick={(e) => { e.stopPropagation(); onSelectDocument(doc); }}
                        className="p-1.5 rounded-lg text-slate-400 hover:text-indigo-400 hover:bg-indigo-500/10 transition"
                      >
                        <ExternalLink className="w-4 h-4" />
                      </button>

                      <button
                        title="Download Original"
                        onClick={(e) => handleDownload(e, doc)}
                        className="p-1.5 rounded-lg text-slate-400 hover:text-emerald-400 hover:bg-emerald-500/10 transition"
                      >
                        <Download className="w-4 h-4" />
                      </button>

                      <button
                        title="Delete Document"
                        disabled={deletingId === doc.id}
                        onClick={(e) => handleDelete(e, doc.id)}
                        className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition disabled:opacity-50"
                      >
                        {deletingId === doc.id ? (
                          <Loader2 className="w-4 h-4 animate-spin" />
                        ) : (
                          <Trash2 className="w-4 h-4" />
                        )}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

    </div>
  );
}
