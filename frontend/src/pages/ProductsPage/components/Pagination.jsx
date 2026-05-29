import React from 'react';
import {FiChevronLeft, FiChevronRight} from 'react-icons/fi';

const Pagination = ({page, totalPages, totalElements, size, onPageChange}) => {
    if (totalPages <= 1) return null;

    const pages = [];
    const maxVisible = 5;
    let start = Math.max(0, page - Math.floor(maxVisible / 2));
    let end = Math.min(totalPages - 1, start + maxVisible - 1);
    if (end - start < maxVisible - 1) {
        start = Math.max(0, end - maxVisible + 1);
    }

    for (let i = start; i <= end; i++) {
        pages.push(i);
    }

    const from = page * size + 1;
    const to = Math.min((page + 1) * size, totalElements);

    return (
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 mt-8">
            <p className="text-sm text-gray-500">
                Показано {from}–{to} из {totalElements} товаров
            </p>
            <div className="flex items-center gap-1">
                <button
                    onClick={() => onPageChange(page - 1)}
                    disabled={page === 0}
                    className="p-2 rounded-lg border border-gray-200 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                >
                    <FiChevronLeft className="w-5 h-5"/>
                </button>

                {start > 0 && (
                    <>
                        <button
                            onClick={() => onPageChange(0)}
                            className="w-9 h-9 rounded-lg border border-gray-200 hover:bg-gray-50 text-sm font-medium transition-colors"
                        >
                            1
                        </button>
                        {start > 1 && <span className="px-1 text-gray-400">…</span>}
                    </>
                )}

                {pages.map(p => (
                    <button
                        key={p}
                        onClick={() => onPageChange(p)}
                        className={`w-9 h-9 rounded-lg border text-sm font-medium transition-colors ${
                            p === page
                                ? 'border-emerald-500 bg-emerald-50 text-emerald-700'
                                : 'border-gray-200 hover:bg-gray-50 text-gray-700'
                        }`}
                    >
                        {p + 1}
                    </button>
                ))}

                {end < totalPages - 1 && (
                    <>
                        {end < totalPages - 2 && <span className="px-1 text-gray-400">…</span>}
                        <button
                            onClick={() => onPageChange(totalPages - 1)}
                            className="w-9 h-9 rounded-lg border border-gray-200 hover:bg-gray-50 text-sm font-medium transition-colors"
                        >
                            {totalPages}
                        </button>
                    </>
                )}

                <button
                    onClick={() => onPageChange(page + 1)}
                    disabled={page >= totalPages - 1}
                    className="p-2 rounded-lg border border-gray-200 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                >
                    <FiChevronRight className="w-5 h-5"/>
                </button>
            </div>
        </div>
    );
};

export default Pagination;
