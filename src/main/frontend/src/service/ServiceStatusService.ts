import axios from 'axios';
import { ServiceStatus } from '@/model/ServiceStatus';
import { ServiceStatusDto } from '@/dto/ServiceStatusDto';

export class ServiceStatusService {
  url = '/api/v1/service';

  public async getAll(): Promise<ServiceStatus[]> {
    const response = await axios({
      method: 'get',
      url: this.url
    });
    return response.data.map((x: ServiceStatusDto) => {
      return {
        url: x.url,
        name: x.name,
        createdAt: new Date(x.created_at),
        status: x.status
      };
    });
  }

  public async delete(name: string): Promise<boolean> {
    const response = await axios({
      url: `${this.url}/${name}`,
      method: 'delete',
      responseType: 'text'
    });

    return response.status === 204;
  }

  public async add(name: string, url: string): Promise<boolean> {
    const response = await axios({
      url: this.url,
      method: 'post',
      data: {
        name: name,
        url: url
      },
      responseType: 'text'
    });

    return response.status === 201;
  }
}
