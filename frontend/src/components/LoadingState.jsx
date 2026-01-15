const LoadingState = () => (
    <div className="min-h-[65vh] flex items-center justify-center">
        <div className="text-center">
            <div className="inline-flex flex-col items-center">
                <div className="relative">
                    <div className="w-20 h-20 border-4 border-emerald-200 rounded-full"></div>
                    <div
                        className="absolute top-0 left-0 w-20 h-20 border-4 border-emerald-600 rounded-full border-t-transparent animate-spin"></div>
                </div>
                <p className="mt-6 text-gray-600 font-medium text-lg">Загрузка...</p>
                <p className="text-sm text-gray-400 mt-1">Пожалуйста, подождите</p>
            </div>
        </div>
    </div>
);

export default LoadingState;