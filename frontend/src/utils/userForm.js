export function buildUserFormData(data) {
    const form = new FormData();
    const {address, ...fields} = data;

    Object.entries(fields).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
            form.append(key, value);
        }
    });

    if (address && Object.values(address).some((v) => v !== undefined && v !== null && v !== '')) {
        Object.entries(address).forEach(([key, value]) => {
            form.append(`address.${key}`, value ?? '');
        });
    }

    return form;
}
