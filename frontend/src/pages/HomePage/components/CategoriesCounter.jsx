const CategoriesCounter = ({count}) => (
    <div className="mt-2 text-center">
        <div className="inline-flex items-center gap-2 px-3 py-1.5 bg-gray-50 rounded-full">
            <span className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse"></span>
            <span className="text-sm text-gray-600 font-medium">
                Всего <span className="text-emerald-600">{count}</span> категорий
            </span>
        </div>
    </div>
);

export default CategoriesCounter;